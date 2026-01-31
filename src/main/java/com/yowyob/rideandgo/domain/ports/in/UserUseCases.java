package com.yowyob.rideandgo.domain.ports.in;

import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.Vehicle; // Import
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.BecomeDriverRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.DriverProfileResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import org.springframework.http.codec.multipart.FilePart;

public interface UserUseCases {
    Mono<User> saveUser(User user);

    Mono<User> getUserById(UUID userId);

    Mono<Boolean> deleteUserById(UUID userId);

    Flux<User> getAllUsers();

    Flux<User> getUsersByRole(RoleType role);

    Mono<Void> upgradeToDriver(UUID userId); // Legacy

    Mono<DriverProfileResponse> upgradeToDriverComplete(UUID userId, BecomeDriverRequest request, FilePart regPhoto,
            FilePart serialPhoto);

    Mono<User> updateProfile(UUID userId, String firstName, String lastName, String phone);

    Mono<Void> changePassword(UUID userId, String currentPassword, String newPassword);

    Flux<User> getAllRemoteUsersByService(String serviceName);
}