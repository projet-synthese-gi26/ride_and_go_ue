package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.domain.ports.in.UpdateLocationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
@Tag(name = "GPS-Tracking", description = "Real-time location updates")
public class LocationController {

    private final UpdateLocationUseCase updateLocationUseCase;

    @PostMapping
    @Operation(summary = "Update current location", description = "Updates the actor's coordinates in Redis. ID is extracted from JWT.")
    public Mono<Boolean> updateLocation(@RequestBody LocationRequest request) {
        return updateLocationUseCase.updateCurrentLocation(request.latitude(), request.longitude());
    }

    public record LocationRequest(Double latitude, Double longitude) {}
}