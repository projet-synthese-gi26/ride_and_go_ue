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
        return externalUserPort.fetchRemoteUserById(userId).flatMap(userRepositoryPort::save);
    }

    @Override
    public Flux<User> getAllUsers() {
        return externalUserPort.fetchAllRemoteUsers().flatMap(userRepositoryPort::save);
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

    // Ancien
    @Override
    public Mono<Void> upgradeToDriver(UUID userId) {
        return driverRepositoryPort.createDriver(userId)
                .flatMap(d -> externalUserPort.addRole(userId, RoleType.RIDE_AND_GO_DRIVER.name()))
                .flatMap(v -> userRepositoryPort.addRoleToUser(userId, RoleType.RIDE_AND_GO_DRIVER))
                .then();
    }

    // Nouveau
    public Mono<Void> upgradeToDriverComplete(UUID userId, BecomeDriverRequest request) {
        log.info("🚀 Starting Driver Onboarding for User {}", userId);

        Vehicle vehicleDomain = Vehicle.builder()
                .vehicleMakeId(request.vehicleMakeName())
                .vehicleModelId(request.vehicleModelName())
                .transmissionTypeId(request.transmissionTypeName())
                .manufacturerId(request.manufacturerName())
                .vehicleSizeId(request.vehicleSizeName())
                .vehicleTypeId(request.vehicleTypeName())
                .fuelTypeId(request.fuelTypeName())
                .vehicleSerialNumber(request.vehicleSerialNumber())
                .registrationNumber(request.registrationNumber())
                .tankCapacity(request.tankCapacity())
                .luggageMaxCapacity(request.luggageMaxCapacity())
                .totalSeatNumber(request.totalSeatNumber())
                .averageFuelConsumptionPerKm(request.averageFuelConsumptionPerKm())
                .mileageAtStart(request.mileageAtStart())
                .mileageSinceCommissioning(request.mileageSinceCommissioning())
                .vehicleAgeAtStart(request.vehicleAgeAtStart())
                .brand(request.vehicleMakeName()) // La marque et le fabricant sont souvent les mêmes
                .build();

        return vehicleRepositoryPort.createVehicle(vehicleDomain)
                .flatMap(createdVehicle -> {
                    log.info("✅ Vehicle created with ID: {}", createdVehicle.id());
                    Driver newDriver = Driver.builder()
                            .id(userId)
                            .status("OFFLINE")
                            .licenseNumber(request.licenseNumber())
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