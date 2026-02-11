package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.application.service.VehicleService;
import com.yowyob.rideandgo.domain.model.Vehicle;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.BecomeDriverRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.UpdateVehicleDto;
import com.yowyob.rideandgo.domain.ports.out.DriverRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicle-Management", description = "Direct Vehicle Operations (External Service Proxy)")
@PreAuthorize("hasAuthority('RIDE_AND_GO_DRIVER')")
public class VehicleController {
    private final VehicleService vehicleService;
    private final DriverRepositoryPort driverRepositoryPort;

    // --- FULL CREATION (JSON + FILES) ---

    @GetMapping("/me")
    @Operation(summary = "Get My Vehicle", description = "Retrieve the vehicle associated with the current driver.")
    public Mono<Vehicle> getMyVehicle() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> UUID.fromString(auth.getName()))
                .flatMap(driverId -> driverRepositoryPort.findById(driverId))
                .flatMap(driver -> {
                    if (driver.vehicleId() == null) {
                        return Mono.error(new IllegalStateException("No vehicle associated with this driver."));
                    }
                    return vehicleService.getVehicleById(driver.vehicleId());
                });
    }

    // --- STANDARD CRUD ---

    @GetMapping("/{id}")
    public Mono<Vehicle> getById(@PathVariable UUID id) {
        return vehicleService.getVehicleById(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Patch Vehicle", description = "Update specific fields of a vehicle.")
    public Mono<Vehicle> patchVehicle(@PathVariable UUID id,
            @RequestBody UpdateVehicleDto dto) {
        // Mapping partiel : on ne mappe que ce qui est présent
        Vehicle partial = Vehicle.builder()
                .vehicleMakeId(dto.makeName())
                .vehicleModelId(dto.modelName())
                .transmissionTypeId(dto.transmissionType())
                .manufacturerId(dto.manufacturerName())
                .vehicleSizeId(dto.sizeName())
                .vehicleTypeId(dto.typeName())
                .fuelTypeId(dto.fuelTypeName())
                .vehicleSerialNumber(dto.vehicleSerialNumber())
                .registrationNumber(dto.registrationNumber())
                .tankCapacity(dto.tankCapacity() != null ? dto.tankCapacity() : 0.0)
                .luggageMaxCapacity(dto.luggageMaxCapacity() != null ? dto.luggageMaxCapacity() : 0.0)
                .totalSeatNumber(dto.totalSeatNumber() != null ? dto.totalSeatNumber() : 0)
                .averageFuelConsumptionPerKm(
                        dto.averageFuelConsumptionPerKm() != null ? dto.averageFuelConsumptionPerKm() : 0.0)
                .mileageAtStart(dto.mileageAtStart() != null ? dto.mileageAtStart() : 0.0)
                .mileageSinceCommissioning(
                        dto.mileageSinceCommissioning() != null ? dto.mileageSinceCommissioning() : 0.0)
                .vehicleAgeAtStart(dto.vehicleAgeAtStart() != null ? dto.vehicleAgeAtStart() : 0.0)
                .brand(dto.makeName())
                .airConditioned(dto.airConditioned() != null ? dto.airConditioned() : false)
                .comfortable(dto.comfortable() != null ? dto.comfortable() : false)
                .soft(dto.soft() != null ? dto.soft() : false)
                .screen(dto.screen() != null ? dto.screen() : false)
                .wifi(dto.wifi() != null ? dto.wifi() : false)
                .tollCharge(dto.tollCharge() != null ? dto.tollCharge() : false)
                .carParking(dto.carParking() != null ? dto.carParking() : false)
                .alarm(dto.alarm() != null ? dto.alarm() : false)
                .stateTax(dto.stateTax() != null ? dto.stateTax() : false)
                .driverAllowance(dto.driverAllowance() != null ? dto.driverAllowance() : false)
                .pickupAndDrop(dto.pickupAndDrop() != null ? dto.pickupAndDrop() : false)
                .internet(dto.internet() != null ? dto.internet() : false)
                .petsAllow(dto.petsAllow() != null ? dto.petsAllow() : false)
                .build();

        return vehicleService.patchVehicle(id, partial);
    }

    // --- MEDIA MANAGEMENT ---

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Gallery Image")
    public Mono<String> uploadImage(@PathVariable UUID id, @RequestPart("file") FilePart file) {
        return vehicleService.addImage(id, file);
    }

    @GetMapping("/{id}/images")
    @Operation(summary = "Get Gallery Images")
    public Flux<String> getImages(@PathVariable UUID id) {
        return vehicleService.getImages(id);
    }

    // --- DOCUMENT UPDATES ---

    @PutMapping(value = "/{id}/documents/registration", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update Registration Photo")
    public Mono<Vehicle> updateRegistrationDoc(@PathVariable UUID id, @RequestPart("file") FilePart file) {
        return vehicleService.createVehicleWithDocuments(
                Vehicle.builder().id(id).build(),
                file, null).flatMap(v -> vehicleService.getVehicleById(id));
    }

    @PutMapping(value = "/{id}/documents/serial", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update Serial Number Photo")
    public Mono<Vehicle> updateSerialDoc(@PathVariable UUID id, @RequestPart("file") FilePart file) {
        return vehicleService.createVehicleWithDocuments(
                Vehicle.builder().id(id).build(),
                null, file).flatMap(v -> vehicleService.getVehicleById(id));
    }
}