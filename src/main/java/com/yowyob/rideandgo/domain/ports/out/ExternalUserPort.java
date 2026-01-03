package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface ExternalUserPort {
    Flux<User> fetchAllRemoteUsers();
    Mono<User> fetchRemoteUserById(UUID id);
}