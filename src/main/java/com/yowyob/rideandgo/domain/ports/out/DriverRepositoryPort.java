package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.Driver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface DriverRepositoryPort {
    Mono<Boolean> setOnlineStatus(UUID driverId, boolean isOnline);

    Flux<String> findDeviceTokensOfOnlineDrivers();

    Flux<String> findEmailsOfEligibleDrivers();

    Mono<Driver> createDriver(UUID userId);

    Mono<Driver> save(Driver driver);

    Mono<Driver> findById(UUID driverId);

    Flux<Driver> findAll();

    Mono<Driver> validateProfile(UUID driverId);

    Flux<Driver> findAllPendingValidation();
}
