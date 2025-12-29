package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.User;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserCachePort {
    Mono<Boolean> saveInCache(User user);

    Mono<User> findUserById(UUID userId);
}
