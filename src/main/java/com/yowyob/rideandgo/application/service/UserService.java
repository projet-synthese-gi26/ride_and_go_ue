package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.in.UserUseCases;
import com.yowyob.rideandgo.domain.ports.out.UserRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.ExternalUserPort;
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
}