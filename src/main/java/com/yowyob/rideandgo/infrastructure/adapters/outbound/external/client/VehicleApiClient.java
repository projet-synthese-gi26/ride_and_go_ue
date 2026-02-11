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
import java.time.LocalDateTime;

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
        Mono<VehicleImageResponse> uploadVehicleImage(@PathVariable String id,
                        @RequestBody MultiValueMap<String, ?> parts);

        @GetExchange("/{id}/images")
        Flux<VehicleImageResponse> getVehicleImages(@PathVariable String id);

        @PutExchange(url = "/{id}/documents/registration", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
        Mono<VehicleResponse> uploadRegistrationDocument(@PathVariable String id,
                        @RequestBody MultiValueMap<String, ?> parts);

        @PutExchange(url = "/{id}/documents/serial", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
        Mono<VehicleResponse> uploadSerialDocument(@PathVariable String id,
                        @RequestBody MultiValueMap<String, ?> parts);

        // --- LOOKUPS (Legacy / Optional now) ---
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
                        LocalDateTime registrationExpiryDate,
                        double tankCapacity,
                        double luggageMaxCapacity,
                        int totalSeatNumber,
                        double averageFuelConsumptionPerKm,
                        double mileageAtStart,
                        double mileageSinceCommissioning,
                        double vehicleAgeAtStart,
                        String brand,
                        boolean airConditioned,
                        boolean comfortable,
                        boolean soft,
                        boolean screen,
                        boolean wifi,
                        boolean tollCharge,
                        boolean carParking,
                        boolean alarm,
                        boolean stateTax,
                        boolean driverAllowance,
                        boolean pickupAndDrop,
                        boolean internet,
                        boolean petsAllow) {
        }

        // 2. Patch Request (IDs partiels) - Utilise des Wrappers (Double, Boolean) pour
        // autoriser le null
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
                        LocalDateTime registrationExpiryDate,
                        Double tankCapacity,
                        Double luggageMaxCapacity,
                        Integer totalSeatNumber,
                        Double averageFuelConsumptionPerKm,
                        Double mileageAtStart,
                        Double mileageSinceCommissioning,
                        Double vehicleAgeAtStart,
                        String brand,
                        Boolean airConditioned,
                        Boolean comfortable,
                        Boolean soft,
                        Boolean screen,
                        Boolean wifi,
                        Boolean tollCharge,
                        Boolean carParking,
                        Boolean alarm,
                        Boolean stateTax,
                        Boolean driverAllowance,
                        Boolean pickupAndDrop,
                        Boolean internet,
                        Boolean petsAllow) {
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
                        LocalDateTime registrationExpiryDate,
                        double tankCapacity,
                        double luggageMaxCapacity,
                        int totalSeatNumber,
                        double averageFuelConsumptionPerKm,
                        double mileageAtStart,
                        double mileageSinceCommissioning,
                        double vehicleAgeAtStart,
                        String brand,
                        LocalDateTime createdAt,
                        LocalDateTime updatedAt,
                        boolean airConditioned,
                        boolean comfortable,
                        boolean soft,
                        boolean screen,
                        boolean wifi,
                        boolean tollCharge,
                        boolean carParking,
                        boolean alarm,
                        boolean stateTax,
                        boolean driverAllowance,
                        boolean pickupAndDrop,
                        boolean internet,
                        boolean petsAllow) {
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