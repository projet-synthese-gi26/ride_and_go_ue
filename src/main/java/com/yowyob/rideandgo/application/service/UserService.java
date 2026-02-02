package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.exception.WalletNotFoundException;
import com.yowyob.rideandgo.domain.model.Driver;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.Vehicle;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.in.UserUseCases;
import com.yowyob.rideandgo.domain.ports.out.*;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.BecomeDriverRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.DriverProfileResponse;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FullDriverProfileResponse;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.UserResponse;
import com.yowyob.rideandgo.infrastructure.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final UserMapper userMapper;

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
                        .map(local -> new User(local.id(), local.name(), local.firstName(), local.lastName(), local.email(), phone, local.password(),
                                local.photoUri(), local.roles(), local.directPermissions()))
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
    public Mono<FullDriverProfileResponse> getFullDriverProfile(UUID userId) {
        log.info("🎯 Fetching Full Aggregated Profile for Driver {}", userId);

        Mono<UserResponse> userMono = getUserById(userId)
                .map(u -> {
                    UserResponse res = userMapper.toResponse(u);
                    if (u.roles() != null) {
                        res.setRoles(u.roles().stream().map(r -> r.type()).collect(Collectors.toList()));
                    }
                    return res;
                });

        Mono<DriverVehicleContainer> driverVehicleMono = driverRepositoryPort.findById(userId)
                .flatMap(driver -> {
                    if (driver.vehicleId() != null) {
                        return vehicleRepositoryPort.getVehicleById(driver.vehicleId())
                                .map(v -> new DriverVehicleContainer(driver, v))
                                .onErrorReturn(new DriverVehicleContainer(driver, null));
                    }
                    return Mono.just(new DriverVehicleContainer(driver, null));
                })
                .defaultIfEmpty(new DriverVehicleContainer(null, null));

        Mono<com.yowyob.rideandgo.domain.model.Wallet> walletMono = paymentPort.getWalletByOwnerId(userId)
                .onErrorResume(e -> {
                    log.warn("Wallet not found for driver profile {}", userId);
                    return Mono.empty();
                });

        return Mono
                .zip(userMono, driverVehicleMono,
                        walletMono.defaultIfEmpty(com.yowyob.rideandgo.domain.model.Wallet.builder().build()))
                .map(tuple -> FullDriverProfileResponse.builder()
                        .user(tuple.getT1())
                        .driver(tuple.getT2().driver)
                        .vehicle(tuple.getT2().vehicle)
                        .wallet(tuple.getT3().id() != null ? tuple.getT3() : null)
                        .build());
    }

    private record DriverVehicleContainer(Driver driver, Vehicle vehicle) {
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
                                        .isProfileValidated(true)
                                        .isSyndicated(false)
                                        .vehicleId(finalVehicle.id())
                                        .build();

                                return driverRepositoryPort.save(newDriver)
                                        .flatMap(savedDriver ->
                                // ✅ VÉRIFICATION WALLET EXISTANT
                                paymentPort.getWalletByOwnerId(userId)
                                        .doOnSuccess(w -> log.info(
                                                "💳 Wallet already exists for driver {}, skipping creation.", userId))
                                        .onErrorResume(WalletNotFoundException.class, e -> {
                                            log.info("💳 Wallet not found for {}, creating new one...", userId);
                                            return paymentPort.createWallet(userId, user.name());
                                        })
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

    @Override
    public Mono<DriverProfileResponse> getDriverProfile(UUID driverId) {
        log.info("🔍 Fetching public profile for driver {}", driverId);

        return Mono.zip(
                driverRepositoryPort.findById(driverId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Chauffeur non trouvé"))),
                userRepositoryPort.findUserById(driverId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Utilisateur non trouvé")))
        ).flatMap(tuple -> {
            Driver driver = tuple.getT1();
            User user = tuple.getT2();

            // Si le chauffeur a un véhicule, on va le chercher, sinon on renvoie null
            return Mono.justOrEmpty(driver.vehicleId())
                    .flatMap(vehicleRepositoryPort::getVehicleById)
                    .map(vehicle -> mapToProfileResponse(user, driver, vehicle))
                    .defaultIfEmpty(mapToProfileResponse(user, driver, null));
        });
    }

    // Helper de mapping pour la lisibilité
    private DriverProfileResponse mapToProfileResponse(User user, Driver driver, Vehicle vehicle) {
        return new DriverProfileResponse(
                user.id(),
                driver.status(),
                driver.licenseNumber(),
                driver.isOnline(),
                driver.isProfileValidated(),
                driver.isSyndicated(),
                driver.isProfileCompleted(),
                vehicle // Contient tous les champs, y compris les images
        );
    }
}