package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.domain.ports.out.DriverRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver-Management", description = "Operations for drivers (status, etc.)")
public class DriverController {

    private final DriverRepositoryPort driverRepositoryPort;

    @PostMapping("/status/online")
    @Operation(summary = "Go Online/Offline", description = "Toggle availability for receiving offers.")
    public Mono<Boolean> setOnlineStatus(@RequestParam boolean isOnline) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    try {
                        UUID driverId = UUID.fromString(auth.getName());
                        return driverRepositoryPort.setOnlineStatus(driverId, isOnline);
                    } catch (Exception e) {
                        return Mono.error(new IllegalStateException("Invalid Token or User ID"));
                    }
                });
    }
}