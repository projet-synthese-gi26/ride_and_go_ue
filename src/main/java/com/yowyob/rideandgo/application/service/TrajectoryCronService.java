package com.yowyob.rideandgo.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.rideandgo.application.utils.Utils;
import com.yowyob.rideandgo.domain.model.DriverTrajectory;
import com.yowyob.rideandgo.domain.ports.out.DriverTrajectoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrajectoryCronService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final DriverTrajectoryRepositoryPort trajectoryRepository;
    private final ObjectMapper objectMapper;

    private static final String PREFIX_HISTORY = "history:driver:";

    @Scheduled(fixedRateString = "${application.trajectory.dump-interval-ms:600000}")
    public void processTrajectories() {
        log.info("⏰ Starting Trajectory Dump (Interval: {}ms)...",
                System.getProperty("application.trajectory.dump-interval-ms", "600000"));

        redisTemplate.scan(ScanOptions.scanOptions().match(PREFIX_HISTORY + "*").build())
                .flatMap(this::processSingleDriverHistory)
                .subscribe(
                        null,
                        e -> log.error("❌ Error during trajectory dump", e),
                        () -> log.info("✅ Trajectory Dump cycle finished."));
    }

    private Mono<Void> processSingleDriverHistory(String key) {
        UUID driverId = UUID.fromString(key.replace(PREFIX_HISTORY, ""));

        // 1. Récupérer tous les points (LRANGE 0 -1)
        return redisTemplate.opsForList().range(key, 0, -1)
                .cast(String.class)
                .collectList()
                .flatMap(points -> {
                    if (points.isEmpty())
                        return Mono.empty();

                    // 2. Supprimer la clé dans Redis immédiatement (Atout: On évite de traiter 2
                    // fois)
                    return redisTemplate.delete(key)
                            .then(saveToPostgres(driverId, points));
                });
    }

    private Mono<Void> saveToPostgres(UUID driverId, List<String> rawPoints) {
        try {
            List<Object[]> trajectoryArray = new ArrayList<>();
            long firstTs = Long.MAX_VALUE;
            long lastTs = Long.MIN_VALUE;

            for (String raw : rawPoints) {
                String[] parts = raw.split(",");
                double lat = Double.parseDouble(parts[0]);
                double lon = Double.parseDouble(parts[1]);
                long ts = Long.parseLong(parts[2]);

                // On prépare le tableau pour le JSON [[lat, lon, ts], ...]
                trajectoryArray.add(new Object[] { lat, lon, ts });

                if (ts < firstTs)
                    firstTs = ts;
                if (ts > lastTs)
                    lastTs = ts;
            }

            // Conversion des timestamps en LocalDateTime
            LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochSecond(firstTs), ZoneId.systemDefault());
            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochSecond(lastTs), ZoneId.systemDefault());

            // Sérialisation du tableau en String JSON
            String json = objectMapper.writeValueAsString(trajectoryArray);

            DriverTrajectory trajectory = DriverTrajectory.builder()
                    .id(Utils.generateUUID())
                    .driverId(driverId)
                    .startTime(start)
                    .endTime(end)
                    .pointsCount(rawPoints.size())
                    .trajectoryDataJson(json)
                    .build();

            return trajectoryRepository.save(trajectory)
                    .doOnSuccess(v -> log.debug("💾 Saved trajectory for driver {} ({} points)", driverId,
                            rawPoints.size()));

        } catch (Exception e) {
            log.error("❌ Error parsing trajectory for driver {}", driverId, e);
            return Mono.empty();
        }
    }
}