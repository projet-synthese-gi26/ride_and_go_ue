package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.ports.out.ExternalUserPort;
import com.yowyob.rideandgo.domain.ports.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class FakeUserAdapter implements ExternalUserPort {

    private final UserRepositoryPort userRepository;

    @Override
    public Flux<User> fetchAllRemoteUsers() {
        // En mode FAKE : On fait semblant que le distant = le local
        return userRepository.findAll();
    }

    @Override
    public Mono<User> fetchRemoteUserById(UUID id) {
        // En mode FAKE : On récupère direct en base locale
        return userRepository.findUserById(id);
    }
}