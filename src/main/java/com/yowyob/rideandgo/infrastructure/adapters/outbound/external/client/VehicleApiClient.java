package com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

@HttpExchange("/vehicles")
public interface VehicleApiClient {

    // Core Vehicle
    @PostExchange
    Mono<VehicleResponse> createVehicle(@RequestBody CreateVehicleRequest request);

    @GetExchange("/{id}")
    Mono<VehicleResponse> getVehicleById(@PathVariable String id);

    // Lookups (GET ALL)
    @GetExchange("/lookup/vehicle-makes")
    Flux<MakeResponse> getAllMakes();
    @GetExchange("/lookup/vehicle-models")
    Flux<ModelResponse> getAllModels();
    @GetExchange("/lookup/transmission-types")
    Flux<TransmissionResponse> getAllTransmissions();
    @GetExchange("/lookup/manufacturers")
    Flux<ManufacturerResponse> getAllManufacturers();
    @GetExchange("/lookup/vehicle-sizes")
    Flux<SizeResponse> getAllSizes();
    @GetExchange("/lookup/vehicle-types")
    Flux<TypeResponse> getAllTypes();
    @GetExchange("/lookup/fuel-types")
    Flux<FuelResponse> getAllFuels();
    
    // Lookups (GET BY ID)
    @GetExchange("/lookup/vehicle-makes/{id}")
    Mono<MakeResponse> getMakeById(@PathVariable String id);
    @GetExchange("/lookup/vehicle-models/{id}")
    Mono<ModelResponse> getModelById(@PathVariable String id);
    @GetExchange("/lookup/vehicle-types/{id}")
    Mono<TypeResponse> getTypeById(@PathVariable String id);

    // Lookups (CREATE)
    @PostExchange("/lookup/vehicle-makes")
    Mono<MakeResponse> createMake(@RequestBody MakeRequest request);
    @PostExchange("/lookup/vehicle-models")
    Mono<ModelResponse> createModel(@RequestBody ModelRequest request);
    @PostExchange("/lookup/transmission-types")
    Mono<TransmissionResponse> createTransmission(@RequestBody TransmissionRequest request);
    @PostExchange("/lookup/manufacturers")
    Mono<ManufacturerResponse> createManufacturer(@RequestBody ManufacturerRequest request);
    @PostExchange("/lookup/vehicle-sizes")
    Mono<SizeResponse> createSize(@RequestBody SizeRequest request);
    @PostExchange("/lookup/vehicle-types")
    Mono<TypeResponse> createType(@RequestBody TypeRequest request);
    @PostExchange("/lookup/fuel-types")
    Mono<FuelResponse> createFuel(@RequestBody FuelRequest request);

    // DTOs
    record MakeRequest(String makeName) {}
    record MakeResponse(String vehicleMakeId, String makeName) {}
    record ModelRequest(String vehicleMakeId, String modelName) {}
    record ModelResponse(String vehicleModelId, String vehicleMakeId, String modelName) {}
    record TransmissionRequest(String typeName) {}
    record TransmissionResponse(String transmissionTypeId, String typeName) {}
    record ManufacturerRequest(String manufacturerName) {}
    record ManufacturerResponse(String manufacturerId, String manufacturerName) {}
    record SizeRequest(String sizeName) {}
    record SizeResponse(String vehicleSizeId, String sizeName) {}
    record TypeRequest(String typeName) {}
    record TypeResponse(String vehicleTypeId, String typeName) {}
    record FuelRequest(String fuelTypeName) {}
    record FuelResponse(String fuelTypeId, String fuelTypeName) {}

    record CreateVehicleRequest(
        String vehicleMakeId, String vehicleModelId, String transmissionTypeId,
        String manufacturerId, String vehicleSizeId, String vehicleTypeId, String fuelTypeId,
        String vehicleSerialNumber, String vehicleSerialPhoto, String registrationNumber,
        String registrationPhoto, int tankCapacity, int luggageMaxCapacity,
        int totalSeatNumber, double averageFuelConsumptionPerKm, int mileageAtStart,
        int mileageSinceCommissioning, int vehicleAgeAtStart, String brand
    ) {}

    record VehicleResponse(
        String vehicleId, String vehicleMakeId, String vehicleModelId, String transmissionTypeId,
        String manufacturerId, String vehicleSizeId, String vehicleTypeId, String fuelTypeId,
        String vehicleSerialNumber, String vehicleSerialPhoto, String registrationNumber,
        String registrationPhoto, ZonedDateTime registrationExpiryDate, int tankCapacity,
        int luggageMaxCapacity, int totalSeatNumber, double averageFuelConsumptionPerKm,
        int mileageAtStart, int mileageSinceCommissioning, int vehicleAgeAtStart,
        String brand, ZonedDateTime createdAt, ZonedDateTime updatedAt
    ) {}
}