package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

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
        return driverRepository.findById(driverId)
                .flatMap(driver -> {
                    if (isOnline && !driver.isProfileValidated()) {
                        log.warn("⛔ Driver {} blocked: Profile not validated by admin.", driverId);
                        return Mono.just(false);
                    }

                    driver.setOnline(isOnline);
                    driver.setNewEntity(false);
                    return driverRepository.save(driver);
                })
                .map(d -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Flux<String> findDeviceTokensOfOnlineDrivers() {
        return driverRepository.findDeviceTokensOfActiveDrivers();
    }

    @Override
    public Mono<Driver> createDriver(UUID userId) {
        // Logique Legacy simple (initialisation vide)
        return driverRepository.findById(userId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("🚀 Creating new Driver profile for User {}", userId);
                    DriverEntity newEntity = new DriverEntity();
                    newEntity.setId(userId);
                    newEntity.setStatus("OFFLINE");
                    newEntity.setOnline(false);
                    newEntity.setProfileCompleted(false);
                    newEntity.setLicenseNumber("PENDING");
                    newEntity.setNewEntity(true); // Force Insert
                    return driverRepository.save(newEntity);
                }))
                .map(this::mapToDomain);
    }

    // --- IMPLEMENTATION ROBUSTE DU SAVE (UPSERT) ---
    @Override
    public Mono<Driver> save(Driver driver) {
        // On mappe le domaine vers l'entité
        DriverEntity entity = new DriverEntity(
                driver.id(),
                driver.status(),
                driver.licenseNumber(),
                driver.hasCar(),
                driver.isOnline(),
                driver.isProfileCompleted(),
                driver.vehicleId(),
                driver.isProfileValidated(),
                false // Par défaut false
        );

        // Vérification explicite : Est-ce que cet ID existe déjà dans la table drivers
        // ?
        return driverRepository.existsById(driver.id())
                .flatMap(exists -> {
                    if (!exists) {
                        // Si n'existe pas -> C'est un INSERT
                        entity.setNewEntity(true);
                        log.debug("Inserting new driver profile {}", driver.id());
                    } else {
                        // Si existe -> C'est un UPDATE
                        entity.setNewEntity(false);
                        log.debug("Updating existing driver profile {}", driver.id());
                    }
                    return driverRepository.save(entity);
                })
                .map(this::mapToDomain);
    }

    @Override
    public Mono<Driver> findById(UUID driverId) {
        return driverRepository.findById(driverId).map(this::mapToDomain);
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
        // Il faut ajouter cette méthode findByIsProfileValidatedFalse dans le Repository interface
        return driverRepository.findByIsProfileValidatedFalse().map(this::mapToDomain);
    }

}