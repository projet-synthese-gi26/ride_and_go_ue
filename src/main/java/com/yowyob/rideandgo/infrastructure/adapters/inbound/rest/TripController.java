package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.application.service.RideService;
import com.yowyob.rideandgo.domain.ports.in.GetRideLocationUseCase;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.RideResponse;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.RideTrackingResponse;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.UpdateStatusRequest;
import com.yowyob.rideandgo.infrastructure.mappers.RideMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Tag(name = "Trip-Controller")
public class TripController {
    private final RideService rideService;
    private final GetRideLocationUseCase getRideLocationUseCase;
    private final RideMapper rideMapper;

    @GetMapping("/history")
    @Operation(summary = "Get my ride history")
    public Flux<RideResponse> getMyHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(auth -> {
                    UUID userId = UUID.fromString(auth.getName());
                    return rideService.getHistoryForUser(userId, page, size);
                })
                .map(rideMapper::toResponse);
    }

    @GetMapping("/driver/{driverId}/history")
    @Operation(summary = "Get driver specific history")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_ADMIN') or #driverId.toString() == authentication.name")
    public Flux<RideResponse> getDriverHistory(
            @PathVariable UUID driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return rideService.getHistoryForDriver(driverId, page, size)
                .map(rideMapper::toResponse);
    }

    @GetMapping("/{id}")
    public Mono<RideResponse> getRideById(@PathVariable UUID id) {
        return rideService.getRideById(id).map(rideMapper::toResponse);
    }

    @GetMapping("/driver/current")
    public Mono<RideResponse> getCurrentRide() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> rideService.getCurrentRideForDriver(UUID.fromString(auth.getName())))
                .map(rideMapper::toResponse);
    }

    @PatchMapping("/{id}/status")
    public Mono<RideResponse> updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> rideService.updateRideStatus(id, request.status(), UUID.fromString(auth.getName())))
                .map(rideMapper::toResponse);
    }

    @GetMapping("/{id}/location")
    public Mono<RideTrackingResponse> getTrackingInfo(@PathVariable UUID id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> getRideLocationUseCase.getPartnerLocation(id, UUID.fromString(auth.getName())));
    }
}