package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.NotificationSettings;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface NotificationSettingsRepositoryPort {
    Mono<NotificationSettings> getSettings(UUID userId);
    Mono<Void> saveSettings(NotificationSettings settings);
}