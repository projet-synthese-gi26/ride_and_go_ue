package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.Driver;
import com.yowyob.rideandgo.domain.ports.out.DriverRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.SendNotificationPort;
import com.yowyob.rideandgo.domain.ports.out.UserDeviceRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.NotificationType;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final DriverRepositoryPort driverRepositoryPort;
    private final SendNotificationPort sendNotificationPort;
    private final UserDeviceRepositoryPort userDeviceRepositoryPort;

    // Injection de l'ID généré (à mettre à jour dans application.yml après exécution du script python)
    @Value("${application.notification.templates.admin-validation:7}")
    private int tmplAccountValidated;

    public Flux<Driver> getPendingDrivers() {
        return driverRepositoryPort.findAllPendingValidation();
    }

    public Mono<Driver> validateDriver(UUID driverId) {
        return driverRepositoryPort.validateProfile(driverId)
                .flatMap(driver -> {
                    log.info("👮 Admin validated driver {}", driverId);
                    
                    // Notification au chauffeur (Async)
                    notifyDriverValidation(driverId).subscribe();
                    
                    return Mono.just(driver);
                });
    }

    private Mono<Void> notifyDriverValidation(UUID driverId) {
        return userDeviceRepositoryPort.findDeviceTokenByUserId(driverId)
                .flatMap(token -> sendNotificationPort.sendNotification(
                        SendNotificationRequest.builder()
                                .notificationType(NotificationType.PUSH) // On privilégie le Push pour l'instant
                                .templateId(tmplAccountValidated)
                                .to(List.of(token))
                                .data(Map.of(
                                    "message", "Votre compte chauffeur a été validé ! Vous pouvez passer en ligne."
                                ))
                                .build()
                ))
                .doOnError(e -> log.warn("Failed to notify driver validation: {}", e.getMessage()))
                .then();
    }
}