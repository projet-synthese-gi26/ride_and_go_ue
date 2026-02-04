package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.model.DriverTrajectory;
import com.yowyob.rideandgo.domain.ports.out.DriverTrajectoryRepositoryPort;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DriverTrajectoryR2dbcAdapter implements DriverTrajectoryRepositoryPort {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Void> save(DriverTrajectory trajectory) {
        String sql = """
                    INSERT INTO ride_and_go.driver_trajectory_history
                    (id, driver_id, start_time, end_time, points_count, trajectory_data)
                    VALUES (:id, :driverId, :start, :end, :count, :data)
                """;

        return databaseClient.sql(sql)
                .bind("id", trajectory.id())
                .bind("driverId", trajectory.driverId())
                .bind("start", trajectory.startTime())
                .bind("end", trajectory.endTime())
                .bind("count", trajectory.pointsCount())
                // Conversion String -> JSONB pour Postgres
                .bind("data", Json.of(trajectory.trajectoryDataJson()))
                .then();
    }

    @Override
    public Flux<DriverTrajectory> findAllByDriverId(UUID driverId) {
        String sql = "SELECT * FROM ride_and_go.driver_trajectory_history WHERE driver_id = :driverId ORDER BY start_time DESC";

        return databaseClient.sql(sql)
                .bind("driverId", driverId)
                .map((row, metadata) -> DriverTrajectory.builder()
                        .id(row.get("id", UUID.class))
                        .driverId(row.get("driver_id", UUID.class))
                        .startTime(row.get("start_time", LocalDateTime.class))
                        .endTime(row.get("end_time", LocalDateTime.class))
                        .pointsCount(row.get("points_count", Integer.class))
                        // On récupère le contenu JSONB sous forme de String
                        .trajectoryDataJson(row.get("trajectory_data", String.class))
                        .build())
                .all();
    }
}