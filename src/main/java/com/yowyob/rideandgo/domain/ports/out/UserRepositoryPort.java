package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface UserRepositoryPort {
    Mono<User> save(User user);

    Mono<Boolean> delete(User user);

    Mono<Boolean> deleteById(UUID userId);

    Mono<Boolean> exists(User user);

    Mono<User> findUserById(UUID userId);

    Flux<User> findByRoleName(RoleType role);

    Flux<User> findAll();

    List<User> findByRoleNameList(RoleType type);
}