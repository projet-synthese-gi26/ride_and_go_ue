package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.exception.DriverProfileNotValidatedException;
import com.yowyob.rideandgo.domain.model.Driver;
import com.yowyob.rideandgo.domain.ports.out.DriverRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.DriverEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.DriverR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DriverR2dbcAdapter implements DriverRepositoryPort {
    private final DriverR2dbcRepository driverRepository;

    @Override
    public Mono<Boolean> setOnlineStatus(UUID driverId, boolean isOnline) {
        log.info("🔌 Request to set ONLINE={} for driver {}", isOnline, driverId);

        return driverRepository.findById(driverId)
                .switchIfEmpty(
                        Mono.error(new IllegalStateException("Le profil chauffeur n'existe pas en base locale.")))
                .flatMap(driverEntity -> {
                    // Blocage métier si on veut passer Online sans validation
                    if (isOnline && !driverEntity.isProfileValidated()) {
                        log.warn("⛔ Driver {} blocked: Profile not validated.", driverId);
                        return Mono.error(new DriverProfileNotValidatedException(
                                "Votre profil n'a pas encore été validé par un administrateur."));
                    }

                    driverEntity.setOnline(isOnline);
                    driverEntity.setNewEntity(false); // Force UPDATE

                    log.info("💾 Saving online status for driver {}", driverId);
                    return driverRepository.save(driverEntity);
                })
                .map(saved -> {
                    log.info("✅ Status updated successfully for {}", driverId);
                    return true;
                });
    }

    @Override
    public Flux<String> findDeviceTokensOfOnlineDrivers() {
        return driverRepository.findDeviceTokensOfActiveDrivers();
    }

    @Override
    public Flux<String> findEmailsOfEligibleDrivers() {
        return driverRepository.findEmailsOfEligibleDrivers();
    }

    @Override
    public Mono<Driver> createDriver(UUID userId) {
        return driverRepository.findById(userId)
                .switchIfEmpty(Mono.defer(() -> {
                    DriverEntity newEntity = new DriverEntity();
                    newEntity.setId(userId);
                    newEntity.setStatus("OFFLINE");
                    newEntity.setOnline(false);
                    newEntity.setProfileCompleted(false);
                    newEntity.setLicenseNumber("PENDING");
                    newEntity.setNewEntity(true);
                    return driverRepository.save(newEntity);
                }))
                .map(this::mapToDomain);
    }

    @Override
    public Mono<Driver> save(Driver driver) {
        DriverEntity entity = new DriverEntity(
                driver.id(), driver.status(), driver.licenseNumber(), driver.hasCar(),
                driver.isOnline(), driver.isProfileCompleted(), driver.vehicleId(),
                driver.isProfileValidated(), driver.isSyndicated(), driver.rating(), driver.totalReviewsCount(), false);
        return driverRepository.existsById(driver.id())
                .flatMap(exists -> {
                    entity.setNewEntity(!exists);
                    return driverRepository.save(entity);
                })
                .map(this::mapToDomain);
    }

    @Override
    public Mono<Driver> findById(UUID driverId) {
        return driverRepository.findById(driverId).map(this::mapToDomain);
    }

    @Override
    public Flux<Driver> findAll() {
        return driverRepository.findAll().map(this::mapToDomain);
    }

    private Driver mapToDomain(DriverEntity entity) {
        return Driver.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .licenseNumber(entity.getLicenseNumber())
                .hasCar(entity.isHasCar())
                .isOnline(entity.isOnline())
                .isProfileCompleted(entity.isProfileCompleted())
                .isProfileValidated(entity.isProfileValidated())
                .isSyndicated(entity.isSyndicated())
                .rating(entity.getRating())
                .totalReviewsCount(entity.getTotalReviewsCount())
                .vehicleId(entity.getVehicleId())
                .build();
    }

    @Override
    public Mono<Driver> validateProfile(UUID driverId) {
        return driverRepository.findById(driverId)
                .flatMap(entity -> {
                    entity.setProfileValidated(true);
                    entity.setNewEntity(false);
                    return driverRepository.save(entity);
                })
                .map(this::mapToDomain);
    }

    @Override
    public Flux<Driver> findAllPendingValidation() {
        return driverRepository.findByIsProfileValidatedFalse().map(this::mapToDomain);
    }
}