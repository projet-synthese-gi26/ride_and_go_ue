package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.ports.in.RegisterDeviceTokenUseCase;
import com.yowyob.rideandgo.domain.ports.out.UserDeviceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService implements RegisterDeviceTokenUseCase {

    private final UserDeviceRepositoryPort deviceRepositoryPort;

    @Override
    public Mono<Void> registerToken(UUID userId, String token, String platform) {
        log.info("📱 Registering device token for user {}", userId);
        return deviceRepositoryPort.saveDeviceToken(userId, token, platform);
    }
}