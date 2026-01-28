package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.domain.model.NotificationSettings;
import com.yowyob.rideandgo.domain.ports.out.NotificationSettingsRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "User preferences configuration")
public class SettingsController {

    private final NotificationSettingsRepositoryPort settingsRepository;

    @GetMapping("/notifications")
    @Operation(summary = "Get notification preferences")
    public Mono<NotificationSettings> getSettings() {
        return getCurrentUserId().flatMap(settingsRepository::getSettings);
    }

    @PutMapping("/notifications")
    @Operation(summary = "Update notification preferences", description = "Enable/Disable specific channels (Email, Push, SMS...)")
    public Mono<Void> updateSettings(@RequestBody SettingsDto request) {
        return getCurrentUserId()
                .flatMap(userId -> settingsRepository.saveSettings(new NotificationSettings(
                        userId,
                        request.email(),
                        request.sms(),
                        request.push(),
                        request.whatsapp())));
    }

    private Mono<UUID> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> UUID.fromString(auth.getName()));
    }

    public record SettingsDto(boolean email, boolean sms, boolean push, boolean whatsapp) {
    }
}