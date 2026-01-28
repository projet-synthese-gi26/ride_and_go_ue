package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.ports.out.UserDeviceRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.UserDeviceEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.UserDeviceR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeviceR2dbcAdapter implements UserDeviceRepositoryPort {

    private final UserDeviceR2dbcRepository repository;

    @Override
    public Mono<Void> saveDeviceToken(UUID userId, String token, String platform) {
        return repository.findById(userId)
                .map(entity -> {
                    entity.setDeviceToken(token);
                    entity.setPlatform(platform);
                    entity.setLastUpdatedAt(LocalDateTime.now());
                    entity.setNewEntity(false); // Update
                    return entity;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    UserDeviceEntity entity = new UserDeviceEntity(userId, token, platform, LocalDateTime.now(), true);
                    return Mono.just(entity);
                }))
                .flatMap(repository::save)
                .then();
    }

    @Override
    public Mono<String> findDeviceTokenByUserId(UUID userId) {
        return repository.findById(userId)
                .map(UserDeviceEntity::getDeviceToken);
    }
}