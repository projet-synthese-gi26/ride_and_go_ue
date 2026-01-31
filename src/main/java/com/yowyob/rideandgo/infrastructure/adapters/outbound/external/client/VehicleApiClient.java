package com.yowyob.rideandgo.infrastructure.adapters.outbound.external.client;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.ZonedDateTime;

@HttpExchange("/vehicles")
public interface VehicleApiClient {
    // --- CORE VEHICLE (New Simplified Flow) ---

    @PostExchange("/simplified")
    Mono<VehicleResponse> createVehicleSimplified(@RequestBody SimplifiedVehicleRequest request);

    @GetExchange("/{id}")
    Mono<VehicleResponse> getVehicleById(@PathVariable String id);

    @PatchExchange("/{id}")
    Mono<VehicleResponse> patchVehicle(@PathVariable String id, @RequestBody UpdateVehicleRequest request);

    // --- MEDIA / DOCUMENTS MANAGEMENT ---

    @PostExchange(url = "/{id}/images", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    Mono<VehicleImageResponse> uploadVehicleImage(@PathVariable String id, @RequestBody MultiValueMap<String, ?> parts);

    @GetExchange("/{id}/images")
    Flux<VehicleImageResponse> getVehicleImages(@PathVariable String id);

    @PutExchange(url = "/{id}/documents/registration", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    Mono<VehicleResponse> uploadRegistrationDocument(@PathVariable String id,
            @RequestBody MultiValueMap<String, ?> parts);

    @PutExchange(url = "/{id}/documents/serial", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    Mono<VehicleResponse> uploadSerialDocument(@PathVariable String id, @RequestBody MultiValueMap<String, ?> parts);

    // --- LOOKUPS (Legacy / Optional now) ---
    // On garde les GET au cas où on voudrait lister les marques dans l'UI
    @GetExchange("/lookup/vehicle-makes")
    Flux<MakeResponse> getAllMakes();

    @GetExchange("/lookup/vehicle-models")
    Flux<ModelResponse> getAllModels();

    @GetExchange("/lookup/vehicle-types")
    Flux<TypeResponse> getAllTypes();

    // --- DTOs ---

    // 1. Simplified Request (Noms au lieu d'IDs)
    record SimplifiedVehicleRequest(
            String makeName,
            String modelName,
            String transmissionType,
            String manufacturerName,
            String sizeName,
            String typeName,
            String fuelTypeName,
            String vehicleSerialNumber,
            String vehicleSerialPhoto,
            String registrationNumber,
            String registrationPhoto,
            ZonedDateTime registrationExpiryDate, // Peut être null
            int tankCapacity,
            int luggageMaxCapacity,
            int totalSeatNumber,
            double averageFuelConsumptionPerKm,
            int mileageAtStart,
            double mileageSinceCommissioning,
            double vehicleAgeAtStart,
            String brand) {
    }

    // 2. Patch Request (IDs partiels)
    record UpdateVehicleRequest(
            String vehicleMakeId,
            String vehicleModelId,
            String transmissionTypeId,
            String manufacturerId,
            String vehicleSizeId,
            String vehicleTypeId,
            String fuelTypeId,
            String vehicleSerialNumber,
            String vehicleSerialPhoto,
            String registrationNumber,
            String registrationPhoto,
            ZonedDateTime registrationExpiryDate,
            Integer tankCapacity,
            Integer luggageMaxCapacity,
            Integer totalSeatNumber,
            Double averageFuelConsumptionPerKm,
            Integer mileageAtStart,
            Double mileageSinceCommissioning,
            Double vehicleAgeAtStart,
            String brand) {
    }

    // 3. Response Standard
    record VehicleResponse(
            String vehicleId,
            String vehicleMakeId,
            String vehicleModelId,
            String transmissionTypeId,
            String manufacturerId,
            String vehicleSizeId,
            String vehicleTypeId,
            String fuelTypeId,
            String vehicleSerialNumber,
            String vehicleSerialPhoto,
            String registrationNumber,
            String registrationPhoto,
            ZonedDateTime registrationExpiryDate,
            int tankCapacity,
            int luggageMaxCapacity,
            int totalSeatNumber,
            double averageFuelConsumptionPerKm,
            int mileageAtStart,
            double mileageSinceCommissioning,
            double vehicleAgeAtStart,
            String brand,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt) {
    }

    // 4. Image Response
    record VehicleImageResponse(
            String vehicleIllustrationImageId,
            String vehicleId,
            String imagePath) {
    }

    // Lookups DTOs
    record MakeResponse(String vehicleMakeId, String makeName) {
    }

    record ModelResponse(String vehicleModelId, String vehicleMakeId, String modelName) {
    }

    record TypeResponse(String vehicleTypeId, String typeName) {
    }
}
