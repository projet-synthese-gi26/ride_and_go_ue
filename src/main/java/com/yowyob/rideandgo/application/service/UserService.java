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

    @Override
    public Mono<DriverProfileResponse> upgradeToDriverComplete(UUID userId, BecomeDriverRequest request,
            FilePart regPhoto, FilePart serialPhoto) {
        log.info("🚀 Starting Driver Onboarding for User {} (Full Flow)", userId);

        // 1. Vérifier d'abord si l'utilisateur a déjà le rôle DRIVER
        return userRepositoryPort.findUserById(userId)
                .flatMap(user -> {
                    boolean alreadyHasRole = user.roles() != null && user.roles().stream()
                            .anyMatch(r -> r.type() == RoleType.RIDE_AND_GO_DRIVER);

                    if (alreadyHasRole) {
                        log.info("ℹ️ User {} already has DRIVER role. Proceeding with vehicle update/creation.",
                                userId);
                    }

                    // 2. Logique de création du véhicule
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
                                log.info("✅ Vehicle created/updated with ID: {}", createdVehicle.id());

                                Mono<Vehicle> chain = Mono.just(createdVehicle);
                                if (regPhoto != null) {
                                    chain = chain.flatMap(
                                            v -> vehicleRepositoryPort.uploadRegistrationDocument(v.id(), regPhoto));
                                }
                                if (serialPhoto != null) {
                                    chain = chain.flatMap(
                                            v -> vehicleRepositoryPort.uploadSerialDocument(v.id(), serialPhoto));
                                }
                                return chain;
                            })
                            .flatMap(finalVehicle -> {
                                // 3. Création / Mise à jour du profil Chauffeur
                                Driver newDriver = Driver.builder()
                                        .id(userId)
                                        .status("OFFLINE")
                                        .licenseNumber(request.licenseNumber())
                                        .hasCar(true)
                                        .isOnline(false)
                                        .isProfileCompleted(true)
                                        .isProfileValidated(false)
                                        .vehicleId(finalVehicle.id())
                                        .build();

                                return driverRepositoryPort.save(newDriver)
                                        .map(savedDriver -> new DriverProfileResponse(
                                                savedDriver.id(),
                                                savedDriver.status(),
                                                savedDriver.licenseNumber(),
                                                savedDriver.isOnline(),
                                                savedDriver.isProfileValidated(),
                                                finalVehicle));
                            })
                            .flatMap(response -> {
                                // 4. Assignation du rôle (Seulement si pas déjà présent)
                                if (!alreadyHasRole) {
                                    return externalUserPort.addRole(userId, RoleType.RIDE_AND_GO_DRIVER.name())
                                            .then(userRepositoryPort.addRoleToUser(userId, RoleType.RIDE_AND_GO_DRIVER))
                                            .doOnSuccess(v -> log.info("🔑 Role DRIVER assigned to user {}", userId))
                                            .thenReturn(response);
                                } else {
                                    return Mono.just(response);
                                }
                            });
                });
    }
}