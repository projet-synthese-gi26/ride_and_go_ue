package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.model.Vehicle;
import com.yowyob.rideandgo.domain.ports.out.VehicleRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client.VehicleApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
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
        log.info("🚗 Orchestrating vehicle creation for: {}", domain.registrationNumber());

        return resolveMake(domain.vehicleMakeId()) // Le domaine contient les noms, pas les IDs. Je corrige.
            .flatMap(makeId -> 
                resolveModel(domain.vehicleModelId(), makeId)
                .flatMap(modelId ->
                    Mono.zip(
                        resolveType(domain.vehicleTypeId()),
                        resolveTransmission(domain.transmissionTypeId()),
                        resolveManufacturer(domain.manufacturerId()),
                        resolveSize(domain.vehicleSizeId()),
                        resolveFuel(domain.fuelTypeId())
                    ).flatMap(tuple -> {
                        var request = new VehicleApiClient.CreateVehicleRequest(
                            makeId, modelId, tuple.getT2(), tuple.getT3(), tuple.getT4(),
                            tuple.getT1(), tuple.getT5(), domain.vehicleSerialNumber(),
                            domain.vehicleSerialPhoto(), domain.registrationNumber(),
                            domain.registrationPhoto(), domain.tankCapacity(), domain.luggageMaxCapacity(),
                            domain.totalSeatNumber(), domain.averageFuelConsumptionPerKm(),
                            domain.mileageAtStart(), domain.mileageSinceCommissioning(),
                            domain.vehicleAgeAtStart(), domain.brand()
                        );
                        
                        return client.createVehicle(request)
                                .map(this::mapResponseToDomain);
                    })
                )
            )
            .flatMap(created -> cacheVehicle(created).thenReturn(created));
    }

    @Override
    public Mono<Vehicle> getVehicleById(UUID vehicleId) {
        String key = CACHE_KEY + vehicleId;
        return redisTemplate.opsForValue().get(key)
                .cast(Vehicle.class)
                .switchIfEmpty(fetchAndEnrich(vehicleId.toString()));
    }

    private Mono<Vehicle> fetchAndEnrich(String id) {
        return client.getVehicleById(id)
            .flatMap(res -> Mono.zip(
                client.getMakeById(res.vehicleMakeId()).map(VehicleApiClient.MakeResponse::makeName).defaultIfEmpty("N/A"),
                client.getModelById(res.vehicleModelId()).map(VehicleApiClient.ModelResponse::modelName).defaultIfEmpty("N/A"),
                client.getTypeById(res.vehicleTypeId()).map(VehicleApiClient.TypeResponse::typeName).defaultIfEmpty("N/A")
            ).map(tuple -> new Vehicle(
                    UUID.fromString(res.vehicleId()),
                    res.vehicleMakeId(), res.vehicleModelId(), res.transmissionTypeId(), res.manufacturerId(),
                    res.vehicleSizeId(), res.vehicleTypeId(), res.fuelTypeId(),
                    res.vehicleSerialNumber(), res.vehicleSerialPhoto(), res.registrationNumber(), res.registrationPhoto(),
                    res.tankCapacity(), res.luggageMaxCapacity(), res.totalSeatNumber(),
                    res.averageFuelConsumptionPerKm(), res.mileageAtStart(), res.mileageSinceCommissioning(),
                    res.vehicleAgeAtStart(), res.brand()
            )))
            .flatMap(v -> cacheVehicle(v).thenReturn(v));
    }

    @Override
    public Mono<Void> cacheVehicle(Vehicle vehicle) {
        return redisTemplate.opsForValue().set(CACHE_KEY + vehicle.id(), vehicle, TTL).then();
    }

    private Mono<String> resolveMake(String name) {
        String safeName = (name == null || name.isEmpty()) ? "Unknown" : name;
        return client.getAllMakes().filter(m -> m.makeName().equalsIgnoreCase(safeName)).next().map(VehicleApiClient.MakeResponse::vehicleMakeId)
            .switchIfEmpty(Mono.defer(() -> client.createMake(new VehicleApiClient.MakeRequest(safeName)).map(VehicleApiClient.MakeResponse::vehicleMakeId)));
    }
    private Mono<String> resolveModel(String name, String makeId) {
        String safeName = (name == null || name.isEmpty()) ? "Unknown" : name;
        return client.getAllModels().filter(m -> m.modelName().equalsIgnoreCase(safeName)).next().map(VehicleApiClient.ModelResponse::vehicleModelId)
            .switchIfEmpty(Mono.defer(() -> client.createModel(new VehicleApiClient.ModelRequest(makeId, safeName)).map(VehicleApiClient.ModelResponse::vehicleModelId)));
    }
    private Mono<String> resolveType(String name) {
        String safeName = (name == null || name.isEmpty()) ? "CAR" : name;
        return client.getAllTypes().filter(t -> t.typeName().equalsIgnoreCase(safeName)).next().map(VehicleApiClient.TypeResponse::vehicleTypeId)
            .switchIfEmpty(Mono.defer(() -> client.createType(new VehicleApiClient.TypeRequest(safeName)).map(VehicleApiClient.TypeResponse::vehicleTypeId)));
    }
    private Mono<String> resolveTransmission(String name) {
        String safeName = (name == null || name.isEmpty()) ? "Manual" : name;
        return client.getAllTransmissions().filter(t -> t.typeName().equalsIgnoreCase(safeName)).next().map(VehicleApiClient.TransmissionResponse::transmissionTypeId)
            .switchIfEmpty(Mono.defer(() -> client.createTransmission(new VehicleApiClient.TransmissionRequest(safeName)).map(VehicleApiClient.TransmissionResponse::transmissionTypeId)));
    }
    private Mono<String> resolveManufacturer(String name) {
        String safeName = (name == null || name.isEmpty()) ? "Unknown" : name;
        return client.getAllManufacturers().filter(m -> m.manufacturerName().equalsIgnoreCase(safeName)).next().map(VehicleApiClient.ManufacturerResponse::manufacturerId)
            .switchIfEmpty(Mono.defer(() -> client.createManufacturer(new VehicleApiClient.ManufacturerRequest(safeName)).map(VehicleApiClient.ManufacturerResponse::manufacturerId)));
    }
    private Mono<String> resolveSize(String name) {
        String safeName = (name == null || name.isEmpty()) ? "Standard" : name;
        return client.getAllSizes().filter(s -> s.sizeName().equalsIgnoreCase(safeName)).next().map(VehicleApiClient.SizeResponse::vehicleSizeId)
            .switchIfEmpty(Mono.defer(() -> client.createSize(new VehicleApiClient.SizeRequest(safeName)).map(VehicleApiClient.SizeResponse::vehicleSizeId)));
    }
    private Mono<String> resolveFuel(String name) {
        String safeName = (name == null || name.isEmpty()) ? "Petrol" : name;
        return client.getAllFuels().filter(f -> f.fuelTypeName().equalsIgnoreCase(safeName)).next().map(VehicleApiClient.FuelResponse::fuelTypeId)
            .switchIfEmpty(Mono.defer(() -> client.createFuel(new VehicleApiClient.FuelRequest(safeName)).map(VehicleApiClient.FuelResponse::fuelTypeId)));
    }
    
    private Vehicle mapResponseToDomain(VehicleApiClient.VehicleResponse res) {
        return new Vehicle(
            UUID.fromString(res.vehicleId()),
            res.vehicleMakeId(), res.vehicleModelId(), res.transmissionTypeId(), res.manufacturerId(),
            res.vehicleSizeId(), res.vehicleTypeId(), res.fuelTypeId(),
            res.vehicleSerialNumber(), res.vehicleSerialPhoto(), res.registrationNumber(), res.registrationPhoto(),
            res.tankCapacity(), res.luggageMaxCapacity(), res.totalSeatNumber(),
            res.averageFuelConsumptionPerKm(), res.mileageAtStart(), res.mileageSinceCommissioning(),
            res.vehicleAgeAtStart(), res.brand()
        );
    }
}