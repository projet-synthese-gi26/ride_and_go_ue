package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.domain.ports.in.RegisterDeviceTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Device-Management", description = "Manage Push Notification Tokens")
public class DeviceController {

    private final RegisterDeviceTokenUseCase registerDeviceTokenUseCase;

    @PostMapping("/token")
    @Operation(summary = "Register FCM/APNS Token", description = "Link a device token to the current user for push notifications.")
    public Mono<Void> registerToken(@RequestBody TokenRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    try {
                        UUID userId = UUID.fromString(auth.getName());
                        return registerDeviceTokenUseCase.registerToken(userId, request.token(), request.platform());
                    } catch (Exception e) {
                        return Mono.error(new IllegalStateException("Invalid User ID"));
                    }
                });
    }

    public record TokenRequest(String token, String platform) {}
}