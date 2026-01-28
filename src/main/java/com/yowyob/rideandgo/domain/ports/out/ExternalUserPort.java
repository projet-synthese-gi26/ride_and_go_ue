package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface ExternalUserPort {
    // Read
    Flux<User> fetchAllRemoteUsers(); // Récupère ceux du service par défaut

    Flux<User> fetchAllRemoteUsersByService(String serviceName); // Récupère pour un service spécifique

    Mono<User> fetchRemoteUserById(UUID id);

    // Write (Propagation)
    Mono<Void> addRole(UUID userId, String roleName);

    Mono<Void> removeRole(UUID userId, String roleName);

    Mono<User> updateProfile(UUID userId, String firstName, String lastName, String phone);

    Mono<Void> changePassword(UUID userId, String currentPassword, String newPassword);
}