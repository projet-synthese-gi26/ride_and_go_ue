package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.model.Vehicle;
import com.yowyob.rideandgo.domain.ports.out.VehicleRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.VehicleApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleAdapter implements VehicleRepositoryPort {
        private final VehicleApiClient client;

        @Override
        public Mono<Vehicle> createVehicle(Vehicle domain) {
                log.info("🚗 [API Direct] Creating vehicle for: {}", domain.registrationNumber());

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
                                domain.mileageSinceCommissioning(),
                                domain.vehicleAgeAtStart(),
                                domain.brand(),
                                domain.airConditioned(),
                                domain.comfortable(),
                                domain.soft(),
                                domain.screen(),
                                domain.wifi(),
                                domain.tollCharge(),
                                domain.carParking(),
                                domain.alarm(),
                                domain.stateTax(),
                                domain.driverAllowance(),
                                domain.pickupAndDrop(),
                                domain.internet(),
                                domain.petsAllow());

                return client.createVehicleSimplified(request)
                                .map(res -> this.mapResponseToDomain(res, Collections.emptyList()))
                                .doOnSuccess(v -> log.info("✅ Vehicle created with ID: {}", v.id()));
        }

        @Override
        public Mono<Vehicle> getVehicleById(UUID vehicleId) {
                if (vehicleId == null) {
                        log.warn("⚠️ Attempted to fetch vehicle with NULL ID");
                        return Mono.empty();
                }

                log.debug("🌐 [API Direct] Fetching vehicle {}", vehicleId);

                return Mono.zip(
                                client.getVehicleById(vehicleId.toString()),
                                this.getVehicleImages(vehicleId).collectList().defaultIfEmpty(Collections.emptyList()))
                                .map(tuple -> mapResponseToDomain(tuple.getT1(), tuple.getT2()))
                                .doOnError(e -> log.error("❌ Failed to fetch vehicle {}: {}", vehicleId,
                                                e.getMessage()));
        }

        @Override
        public Mono<Vehicle> patchVehicle(UUID vehicleId, Vehicle partial) {
                log.info("🔧 [API Direct] Patching vehicle {}", vehicleId);

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
                                partial.averageFuelConsumptionPerKm() > 0 ? partial.averageFuelConsumptionPerKm()
                                                : null,
                                partial.mileageAtStart() > 0 ? partial.mileageAtStart() : null,
                                partial.mileageSinceCommissioning() > 0 ? partial.mileageSinceCommissioning() : null,
                                partial.vehicleAgeAtStart() > 0 ? partial.vehicleAgeAtStart() : null,
                                partial.brand(),
                                partial.airConditioned(),
                                partial.comfortable(),
                                partial.soft(),
                                partial.screen(),
                                partial.wifi(),
                                partial.tollCharge(),
                                partial.carParking(),
                                partial.alarm(),
                                partial.stateTax(),
                                partial.driverAllowance(),
                                partial.pickupAndDrop(),
                                partial.internet(),
                                partial.petsAllow());

                return client.patchVehicle(vehicleId.toString(), request)
                                .flatMap(res -> this.getVehicleImages(vehicleId).collectList()
                                                .map(images -> mapResponseToDomain(res, images)));
        }

        @Override
        public Mono<Vehicle> uploadRegistrationDocument(UUID vehicleId, FilePart file) {
                return client.uploadRegistrationDocument(vehicleId.toString(), buildMultipart(file, "file"))
                                .flatMap(res -> this.getVehicleImages(vehicleId).collectList()
                                                .map(images -> mapResponseToDomain(res, images)));
        }

        @Override
        public Mono<Vehicle> uploadSerialDocument(UUID vehicleId, FilePart file) {
                return client.uploadSerialDocument(vehicleId.toString(), buildMultipart(file, "file"))
                                .flatMap(res -> this.getVehicleImages(vehicleId).collectList()
                                                .map(images -> mapResponseToDomain(res, images)));
        }

        @Override
        public Mono<String> uploadVehicleImage(UUID vehicleId, FilePart file) {
                return client.uploadVehicleImage(vehicleId.toString(), buildMultipart(file, "file"))
                                .map(VehicleApiClient.VehicleImageResponse::imagePath);
        }

        @Override
        public Flux<String> getVehicleImages(UUID vehicleId) {
                return client.getVehicleImages(vehicleId.toString())
                                .map(VehicleApiClient.VehicleImageResponse::imagePath)
                                .onErrorResume(e -> {
                                        log.warn("Could not fetch images for vehicle {}: {}", vehicleId,
                                                        e.getMessage());
                                        return Flux.empty();
                                });
        }

        @Override
        public Mono<Void> cacheVehicle(Vehicle vehicle) {
                return Mono.empty();
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
                                res.mileageSinceCommissioning(),
                                res.vehicleAgeAtStart(),
                                res.brand(),
                                images,
                                res.airConditioned(),
                                res.comfortable(),
                                res.soft(),
                                res.screen(),
                                res.wifi(),
                                res.tollCharge(),
                                res.carParking(),
                                res.alarm(),
                                res.stateTax(),
                                res.driverAllowance(),
                                res.pickupAndDrop(),
                                res.internet(),
                                res.petsAllow());
        }

        private MultiValueMap<String, ?> buildMultipart(FilePart file, String keyName) {
                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part(keyName, file);
                return builder.build();
        }
}