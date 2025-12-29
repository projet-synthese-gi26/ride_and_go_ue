package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.Fare;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface FareCachePort {
    Mono<Boolean> saveInCache(Fare fare);

    Mono<Fare> findFareById(UUID fareId);
}