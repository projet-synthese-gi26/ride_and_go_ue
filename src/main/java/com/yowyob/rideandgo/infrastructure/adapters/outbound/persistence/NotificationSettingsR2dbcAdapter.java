package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.model.NotificationSettings;
import com.yowyob.rideandgo.domain.ports.out.NotificationSettingsRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.NotificationSettingsEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.NotificationSettingsR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationSettingsR2dbcAdapter implements NotificationSettingsRepositoryPort {

    private final NotificationSettingsR2dbcRepository repository;

    @Override
    public Mono<NotificationSettings> getSettings(UUID userId) {
        return repository.findById(userId)
                .map(this::toDomain)
                // Si pas de settings en base, on retourne les défauts
                .defaultIfEmpty(NotificationSettings.defaults(userId));
    }

    @Override
    public Mono<Void> saveSettings(NotificationSettings settings) {
        return repository.findById(settings.userId())
                .map(entity -> {
                    updateEntity(entity, settings);
                    entity.setNewEntity(false);
                    return entity;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    NotificationSettingsEntity entity = new NotificationSettingsEntity();
                    entity.setUserId(settings.userId());
                    updateEntity(entity, settings);
                    entity.setNewEntity(true);
                    return Mono.just(entity);
                }))
                .flatMap(repository::save)
                .then();
    }

    private void updateEntity(NotificationSettingsEntity entity, NotificationSettings domain) {
        entity.setEnableEmail(domain.emailEnabled());
        entity.setEnablePush(domain.pushEnabled());
        entity.setEnableSms(domain.smsEnabled());
        entity.setEnableWhatsapp(domain.whatsappEnabled());
    }

    private NotificationSettings toDomain(NotificationSettingsEntity entity) {
        return new NotificationSettings(
            entity.getUserId(),
            entity.isEnableEmail(),
            entity.isEnableSms(),
            entity.isEnablePush(),
            entity.isEnableWhatsapp()
        );
    }
}