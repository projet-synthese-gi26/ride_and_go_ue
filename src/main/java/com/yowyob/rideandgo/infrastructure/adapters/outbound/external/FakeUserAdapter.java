package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.ports.out.ExternalUserPort;
import com.yowyob.rideandgo.domain.ports.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class FakeUserAdapter implements ExternalUserPort {

    private final UserRepositoryPort userRepository;

    @Override
    public Flux<User> fetchAllRemoteUsers() {
        return userRepository.findAll();
    }

    @Override
    public Flux<User> fetchAllRemoteUsersByService(String serviceName) {
        // En mode fake, on ignore le filtre service, on renvoie tout le local
        return userRepository.findAll();
    }

    @Override
    public Mono<User> fetchRemoteUserById(UUID id) {
        return userRepository.findUserById(id);
    }

    // --- WRITE OPERATIONS (Fake Implementation) ---

    @Override
    public Mono<Void> addRole(UUID userId, String roleName) {
        log.info("🛠 FAKE ADAPTER: Pretending to add role {} to user {}", roleName, userId);
        return Mono.empty();
    }

    @Override
    public Mono<Void> removeRole(UUID userId, String roleName) {
        log.info("🛠 FAKE ADAPTER: Pretending to remove role {} from user {}", roleName, userId);
        return Mono.empty();
    }

    @Override
    public Mono<User> updateProfile(UUID userId, String firstName, String lastName, String phone) {
        log.info("🛠 FAKE ADAPTER: Pretending to update profile for user {}", userId);
        // On retourne l'utilisateur local tel quel
        return userRepository.findUserById(userId);
    }

    @Override
    public Mono<Void> changePassword(UUID userId, String currentPassword, String newPassword) {
        log.info("🛠 FAKE ADAPTER: Pretending to change password for user {}", userId);
        return Mono.empty();
    }
}