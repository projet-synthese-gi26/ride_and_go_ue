package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.Driver;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.Vehicle;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.in.UserUseCases;
import com.yowyob.rideandgo.domain.ports.out.UserRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.ExternalUserPort;
import com.yowyob.rideandgo.domain.ports.out.DriverRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.VehicleRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.BecomeDriverRequest; // Import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserUseCases {
    
    private final UserRepositoryPort userRepositoryPort;
    private final ExternalUserPort externalUserPort;
    private final DriverRepositoryPort driverRepositoryPort;
    private final VehicleRepositoryPort vehicleRepositoryPort;

    @Override
    public Mono<User> saveUser(User user) {
        return userRepositoryPort.save(user);
    }

    @Override
    public Mono<Boolean> deleteUserById(UUID userId) {
        return userRepositoryPort.deleteById(userId);
    }

    @Override
    public Flux<User> getUsersByRole(RoleType role) {
        return userRepositoryPort.findByRoleName(role);
    }

    @Override
    public Mono<User> getUserById(UUID userId) {
        // Règle stricte : Appel Distant -> Sauvegarde Locale -> Retour
        return externalUserPort.fetchRemoteUserById(userId)
                .flatMap(userRepositoryPort::save)
                .doOnSuccess(u -> log.info("✅ Synced user {} from remote", u.id()));
    }

    @Override
    public Flux<User> getAllUsers() {
        // Appel Distant -> Sauvegarde tout -> Retourne flux sauvegardé
        return externalUserPort.fetchAllRemoteUsers()
                .flatMap(userRepositoryPort::save)
                .doOnComplete(() -> log.info("✅ Full user list synced from remote"));
    }

    @Override
    public Mono<User> updateProfile(UUID userId, String firstName, String lastName, String phone) {
        return externalUserPort.updateProfile(userId, firstName, lastName, phone)
                .flatMap(updated -> userRepositoryPort.findUserById(userId)
                        .map(local -> new User(local.id(), local.name(), local.email(), phone, local.password(),
                                local.roles(), local.directPermissions()))
                        .flatMap(userRepositoryPort::save));
    }

    @Override
    public Mono<Void> changePassword(UUID userId, String currentPassword, String newPassword) {
        return externalUserPort.changePassword(userId, currentPassword, newPassword);
    }

    @Override
    public Flux<User> getAllRemoteUsersByService(String serviceName) {
        return externalUserPort.fetchAllRemoteUsersByService(serviceName).flatMap(userRepositoryPort::save);
    }

    // Ancien (Legacy)
    @Override
    public Mono<Void> upgradeToDriver(UUID userId) {
        return driverRepositoryPort.createDriver(userId)
                .flatMap(d -> externalUserPort.addRole(userId, RoleType.RIDE_AND_GO_DRIVER.name()))
                .flatMap(v -> userRepositoryPort.addRoleToUser(userId, RoleType.RIDE_AND_GO_DRIVER))
                .then();
    }

    // Nouveau (Complet avec Véhicule imbriqué)
    public Mono<Void> upgradeToDriverComplete(UUID userId, BecomeDriverRequest request) {
        log.info("🚀 Starting Driver Onboarding for User {}", userId);

        // Extraction des données du sous-objet vehicle
        var vInfo = request.vehicle();

        Vehicle vehicleDomain = Vehicle.builder()
                .vehicleMakeId(vInfo.vehicleMakeName())
                .vehicleModelId(vInfo.vehicleModelName())
                .transmissionTypeId(vInfo.transmissionTypeName())
                .manufacturerId(vInfo.manufacturerName())
                .vehicleSizeId(vInfo.vehicleSizeName())
                .vehicleTypeId(vInfo.vehicleTypeName())
                .fuelTypeId(vInfo.fuelTypeName())
                .vehicleSerialNumber(vInfo.vehicleSerialNumber())
                .registrationNumber(vInfo.registrationNumber())
                .tankCapacity(vInfo.tankCapacity())
                .luggageMaxCapacity(vInfo.luggageMaxCapacity())
                .totalSeatNumber(vInfo.totalSeatNumber())
                .averageFuelConsumptionPerKm(vInfo.averageFuelConsumptionPerKm())
                .mileageAtStart(vInfo.mileageAtStart())
                .mileageSinceCommissioning(vInfo.mileageSinceCommissioning())
                .vehicleAgeAtStart(vInfo.vehicleAgeAtStart())
                .brand(vInfo.vehicleMakeName()) // La marque et le fabricant sont souvent les mêmes
                .build();

        return vehicleRepositoryPort.createVehicle(vehicleDomain)
                .flatMap(createdVehicle -> {
                    log.info("✅ Vehicle created with ID: {}", createdVehicle.id());
                    Driver newDriver = Driver.builder()
                            .id(userId)
                            .status("OFFLINE")
                            .licenseNumber(request.licenseNumber()) // Info chauffeur
                            .hasCar(true)
                            .isOnline(false)
                            .isProfileCompleted(true)
                            .isProfileValidated(false)
                            .vehicleId(createdVehicle.id())
                            .build();
                    return driverRepositoryPort.save(newDriver);
                })
                .flatMap(d -> externalUserPort.addRole(userId, RoleType.RIDE_AND_GO_DRIVER.name()))
                .flatMap(v -> userRepositoryPort.addRoleToUser(userId, RoleType.RIDE_AND_GO_DRIVER))
                .doOnSuccess(v -> log.info("🏁 Onboarding Complete for User {}", userId))
                .then();
    }
}