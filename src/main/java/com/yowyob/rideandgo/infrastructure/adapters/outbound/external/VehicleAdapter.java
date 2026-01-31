package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.model.Vehicle;
import com.yowyob.rideandgo.domain.ports.out.VehicleRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.VehicleApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleAdapter implements VehicleRepositoryPort {
    private final VehicleApiClient client;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY = "vehicle:";
    private static final Duration TTL = Duration.ofHours(1);

    @Override
    public Mono<Vehicle> createVehicle(Vehicle domain) {
        log.info("🚗 Creating vehicle via Simplified API for: {}", domain.registrationNumber());

        var request = new VehicleApiClient.SimplifiedVehicleRequest(
                domain.vehicleMakeId(),
                domain.vehicleModelId(),
                domain.transmissionTypeId(),
                domain.manufacturerId(),
                domain.vehicleSizeId(),
                domain.vehicleTypeId(),
                domain.fuelTypeId(),
                domain.vehicleSerialNumber(),
                domain.vehicleSerialPhoto(),
                domain.registrationNumber(),
                domain.registrationPhoto(),
                null,
                domain.tankCapacity(),
                domain.luggageMaxCapacity(),
                domain.totalSeatNumber(),
                domain.averageFuelConsumptionPerKm(),
                domain.mileageAtStart(),
                (double) domain.mileageSinceCommissioning(),
                (double) domain.vehicleAgeAtStart(),
                domain.brand());

        return client.createVehicleSimplified(request)
                .map(res -> this.mapResponseToDomain(res, Collections.emptyList()))
                .flatMap(created -> cacheVehicle(created).thenReturn(created))
                .doOnSuccess(v -> log.info("✅ Vehicle created with ID: {}", v.id()));
    }

    @Override
    public Mono<Vehicle> getVehicleById(UUID vehicleId) {
        String key = CACHE_KEY + vehicleId;
        return redisTemplate.opsForValue().get(key)
                .cast(Vehicle.class)
                .switchIfEmpty(fetchAndCache(vehicleId.toString()));
    }

    @Override
    public Mono<Vehicle> patchVehicle(UUID vehicleId, Vehicle partial) {
        log.info("🔧 Patching vehicle {}", vehicleId);

        var request = new VehicleApiClient.UpdateVehicleRequest(
                partial.vehicleMakeId(),
                partial.vehicleModelId(),
                partial.transmissionTypeId(),
                partial.manufacturerId(),
                partial.vehicleSizeId(),
                partial.vehicleTypeId(),
                partial.fuelTypeId(),
                partial.vehicleSerialNumber(),
                partial.vehicleSerialPhoto(),
                partial.registrationNumber(),
                partial.registrationPhoto(),
                null,
                partial.tankCapacity() > 0 ? partial.tankCapacity() : null,
                partial.luggageMaxCapacity() > 0 ? partial.luggageMaxCapacity() : null,
                partial.totalSeatNumber() > 0 ? partial.totalSeatNumber() : null,
                partial.averageFuelConsumptionPerKm() > 0 ? partial.averageFuelConsumptionPerKm() : null,
                partial.mileageAtStart() > 0 ? partial.mileageAtStart() : null,
                partial.mileageSinceCommissioning() > 0 ? (double) partial.mileageSinceCommissioning() : null,
                partial.vehicleAgeAtStart() > 0 ? (double) partial.vehicleAgeAtStart() : null,
                partial.brand());

        return client.patchVehicle(vehicleId.toString(), request)
                .flatMap(res -> this.getVehicleImages(vehicleId).collectList()
                        .map(images -> mapResponseToDomain(res, images)))
                .flatMap(updated -> cacheVehicle(updated).thenReturn(updated));
    }

    @Override
    public Mono<Vehicle> uploadRegistrationDocument(UUID vehicleId, FilePart file) {
        return client.uploadRegistrationDocument(vehicleId.toString(), buildMultipart(file, "file"))
                .flatMap(res -> this.getVehicleImages(vehicleId).collectList()
                        .map(images -> mapResponseToDomain(res, images)))
                .flatMap(v -> cacheVehicle(v).thenReturn(v));
    }

    @Override
    public Mono<Vehicle> uploadSerialDocument(UUID vehicleId, FilePart file) {
        return client.uploadSerialDocument(vehicleId.toString(), buildMultipart(file, "file"))
                .flatMap(res -> this.getVehicleImages(vehicleId).collectList()
                        .map(images -> mapResponseToDomain(res, images)))
                .flatMap(v -> cacheVehicle(v).thenReturn(v));
    }

    @Override
    public Mono<String> uploadVehicleImage(UUID vehicleId, FilePart file) {
        return client.uploadVehicleImage(vehicleId.toString(), buildMultipart(file, "file"))
                .map(VehicleApiClient.VehicleImageResponse::imagePath)
                // Invalidation du cache pour forcer le rechargement des images au prochain GET
                .flatMap(path -> redisTemplate.delete(CACHE_KEY + vehicleId).thenReturn(path));
    }

    @Override
    public Flux<String> getVehicleImages(UUID vehicleId) {
        return client.getVehicleImages(vehicleId.toString())
                .map(VehicleApiClient.VehicleImageResponse::imagePath);
    }

    // --- HELPERS ---

    private Mono<Vehicle> fetchAndCache(String id) {
        return Mono.zip(
                client.getVehicleById(id),
                this.getVehicleImages(UUID.fromString(id)).collectList().defaultIfEmpty(Collections.emptyList()))
                .map(tuple -> mapResponseToDomain(tuple.getT1(), tuple.getT2()))
                .flatMap(v -> cacheVehicle(v).thenReturn(v));
    }

    @Override
    public Mono<Void> cacheVehicle(Vehicle vehicle) {
        return redisTemplate.opsForValue().set(CACHE_KEY + vehicle.id(), vehicle, TTL).then();
    }

    private Vehicle mapResponseToDomain(VehicleApiClient.VehicleResponse res, List<String> images) {
        return new Vehicle(
                UUID.fromString(res.vehicleId()),
                res.vehicleMakeId(),
                res.vehicleModelId(),
                res.transmissionTypeId(),
                res.manufacturerId(),
                res.vehicleSizeId(),
                res.vehicleTypeId(),
                res.fuelTypeId(),
                res.vehicleSerialNumber(),
                res.vehicleSerialPhoto(),
                res.registrationNumber(),
                res.registrationPhoto(),
                res.tankCapacity(),
                res.luggageMaxCapacity(),
                res.totalSeatNumber(),
                res.averageFuelConsumptionPerKm(),
                res.mileageAtStart(),
                (int) res.mileageSinceCommissioning(),
                (int) res.vehicleAgeAtStart(),
                res.brand(),
                images);
    }

    private MultiValueMap<String, ?> buildMultipart(FilePart file, String keyName) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part(keyName, file);
        return builder.build();
    }
}
