package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.Driver;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.Vehicle;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.in.UserUseCases;
import com.yowyob.rideandgo.domain.ports.out.*;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.BecomeDriverRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.DriverProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
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
    private final SyndicatePort syndicatePort;
    private final PaymentPort paymentPort;

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

    @Override
    public Mono<Void> upgradeToDriver(UUID userId) {
        return driverRepositoryPort.createDriver(userId)
                .flatMap(d -> externalUserPort.addRole(userId, RoleType.RIDE_AND_GO_DRIVER.name()))
                .flatMap(v -> userRepositoryPort.addRoleToUser(userId, RoleType.RIDE_AND_GO_DRIVER))
                .flatMap(v -> paymentPort.createWallet(userId, "Driver_" + userId.toString().substring(0, 5)))
                .then();
    }

    @Override
    public Mono<DriverProfileResponse> upgradeToDriverComplete(UUID userId, BecomeDriverRequest request,
            FilePart regPhoto, FilePart serialPhoto) {
        log.info("🚀 Starting Driver Onboarding for User {} (Full Flow)", userId);

        return userRepositoryPort.findUserById(userId)
                .flatMap(user -> {
                    boolean alreadyHasRole = user.roles() != null && user.roles().stream()
                            .anyMatch(r -> r.type() == RoleType.RIDE_AND_GO_DRIVER);

                    var vInfo = request.vehicle();
                    Vehicle vehicleDomain = Vehicle.builder()
                            .vehicleMakeId(vInfo.makeName())
                            .vehicleModelId(vInfo.modelName())
                            .transmissionTypeId(vInfo.transmissionType())
                            .manufacturerId(vInfo.manufacturerName())
                            .vehicleSizeId(vInfo.sizeName())
                            .vehicleTypeId(vInfo.typeName())
                            .fuelTypeId(vInfo.fuelTypeName())
                            .vehicleSerialNumber(vInfo.vehicleSerialNumber())
                            .registrationNumber(vInfo.registrationNumber())
                            .tankCapacity(vInfo.tankCapacity())
                            .luggageMaxCapacity(vInfo.luggageMaxCapacity())
                            .totalSeatNumber(vInfo.totalSeatNumber())
                            .averageFuelConsumptionPerKm(vInfo.averageFuelConsumptionPerKm())
                            .mileageAtStart(vInfo.mileageAtStart())
                            .mileageSinceCommissioning((int) vInfo.mileageSinceCommissioning())
                            .vehicleAgeAtStart((int) vInfo.vehicleAgeAtStart())
                            .brand(vInfo.makeName())
                            .build();

                    return vehicleRepositoryPort.createVehicle(vehicleDomain)
                            .flatMap(createdVehicle -> {
                                Mono<Vehicle> chain = Mono.just(createdVehicle);
                                if (regPhoto != null)
                                    chain = chain.flatMap(
                                            v -> vehicleRepositoryPort.uploadRegistrationDocument(v.id(), regPhoto));
                                if (serialPhoto != null)
                                    chain = chain.flatMap(
                                            v -> vehicleRepositoryPort.uploadSerialDocument(v.id(), serialPhoto));
                                return chain;
                            })
                            .flatMap(finalVehicle -> {
                                Driver newDriver = Driver.builder()
                                        .id(userId)
                                        .status("OFFLINE")
                                        .licenseNumber(request.licenseNumber())
                                        .hasCar(true)
                                        .isOnline(false)
                                        .isProfileCompleted(false)
                                        .isProfileValidated(false)
                                        .isSyndicated(false)
                                        .vehicleId(finalVehicle.id())
                                        .build();

                                return driverRepositoryPort.save(newDriver)
                                        // CAS 2 : Création Wallet lors de l'onboarding
                                        .flatMap(savedDriver -> paymentPort.createWallet(userId, user.name())
                                                .thenReturn(savedDriver))
                                        .map(savedDriver -> new DriverProfileResponse(
                                                savedDriver.id(),
                                                savedDriver.status(),
                                                savedDriver.licenseNumber(),
                                                savedDriver.isOnline(),
                                                savedDriver.isProfileValidated(),
                                                savedDriver.isSyndicated(),
                                                savedDriver.isProfileCompleted(),
                                                finalVehicle));
                            })
                            .flatMap(response -> {
                                if (!alreadyHasRole) {
                                    return externalUserPort.addRole(userId, RoleType.RIDE_AND_GO_DRIVER.name())
                                            .then(userRepositoryPort.addRoleToUser(userId, RoleType.RIDE_AND_GO_DRIVER))
                                            .thenReturn(response);
                                } else {
                                    return Mono.just(response);
                                }
                            });
                });
    }

    @Override
    public Mono<DriverProfileResponse> verifySyndicateStatus(UUID userId) {
        log.info("🛠 Verifying Syndicate status for Driver {}", userId);

        return syndicatePort.checkIsSyndicated(userId)
                .flatMap(isVerified -> driverRepositoryPort.findById(userId)
                        .flatMap(driver -> {
                            Driver updatedDriver = Driver.builder()
                                    .id(driver.id())
                                    .status(driver.status())
                                    .licenseNumber(driver.licenseNumber())
                                    .hasCar(driver.hasCar())
                                    .isOnline(driver.isOnline())
                                    .isProfileValidated(driver.isProfileValidated())
                                    .vehicleId(driver.vehicleId())
                                    .isSyndicated(isVerified)
                                    .isProfileCompleted(isVerified)
                                    .build();

                            return driverRepositoryPort.save(updatedDriver)
                                    .flatMap(saved -> vehicleRepositoryPort.getVehicleById(saved.vehicleId())
                                            .map(v -> new DriverProfileResponse(
                                                    saved.id(), saved.status(), saved.licenseNumber(),
                                                    saved.isOnline(), saved.isProfileValidated(),
                                                    saved.isSyndicated(), saved.isProfileCompleted(), v)));
                        }));
    }
}