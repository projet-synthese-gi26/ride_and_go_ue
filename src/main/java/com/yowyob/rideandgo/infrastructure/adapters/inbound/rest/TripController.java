package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.application.service.RideService;
import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.RideResponse;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.UpdateStatusRequest;
import com.yowyob.rideandgo.infrastructure.mappers.RideMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Tag(name = "Trip-Controller", description = "Ride lifecycle management (Driver)")
public class TripController {

    private final RideService rideService; // Injection de la classe concrète pour avoir accès aux 2 méthodes
    private final RideMapper rideMapper;

    @GetMapping("/driver/current")
    @Operation(summary = "Get current active ride for driver", description = "Finds ride in CREATED or ONGOING state")
    public Mono<RideResponse> getCurrentRide() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    try {
                        UUID driverId = UUID.fromString(auth.getName());
                        return rideService.getCurrentRideForDriver(driverId);
                    } catch (Exception e) {
                        // Aide au compilateur: typer explicitement le retour d'erreur
                        return Mono.<Ride>error(new IllegalStateException("Invalid Token"));
                    }
                })
                .map(rideMapper::toResponse);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update ride status", description = "Transitions: CREATED -> ONGOING -> COMPLETED")
    public Mono<RideResponse> updateStatus(
            @PathVariable UUID id, 
            @RequestBody UpdateStatusRequest request) {
        
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    try {
                        UUID actorId = UUID.fromString(auth.getName());
                        // On appelle le service
                        return rideService.updateRideStatus(id, request.status(), actorId);
                    } catch (IllegalArgumentException e) {
                        // FIX: On force le type <Ride> pour que le flatMap ne soit pas perdu
                        return Mono.<Ride>error(new IllegalStateException("Invalid User ID in Token"));
                    }
                })
                .map(rideMapper::toResponse);
    }
}