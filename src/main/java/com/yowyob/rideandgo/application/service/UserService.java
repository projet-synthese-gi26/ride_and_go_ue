package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.in.UserUseCases;
import com.yowyob.rideandgo.domain.ports.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserUseCases {

    private final UserRepositoryPort userRepositoryPort;

    @Override
    public Mono<User> saveUser(User user) {
        return userRepositoryPort.save(user);
    }

    @Override
    public Mono<User> getUserById(UUID userId) {
        return userRepositoryPort.findUserById(userId);
    }

    @Override
    public Mono<Boolean> deleteUserById(UUID userId) {
        return userRepositoryPort.deleteById(userId);
    }

    @Override
    public Flux<User> getAllUsers() {
        return userRepositoryPort.findAll();
    }

    @Override
    public Flux<User> getUsersByRole(RoleType role) {
        return userRepositoryPort.findByRoleName(role);
    }
}