package com.yowyob.rideandgo.domain.ports.in;

import reactor.core.publisher.Mono;
import java.util.UUID;

public interface RegisterDeviceTokenUseCase {
    Mono<Void> registerToken(UUID userId, String token, String platform);
}