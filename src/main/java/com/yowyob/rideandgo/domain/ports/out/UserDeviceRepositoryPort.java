package com.yowyob.rideandgo.domain.ports.out;

import reactor.core.publisher.Mono;
import java.util.UUID;

public interface UserDeviceRepositoryPort {
    Mono<Void> saveDeviceToken(UUID userId, String token, String platform);

    Mono<String> findDeviceTokenByUserId(UUID userId);
}