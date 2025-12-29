package com.yowyob.rideandgo.domain.ports.in;

import com.yowyob.rideandgo.domain.model.Fare;
import reactor.core.publisher.Mono;

public interface PutFareInCacheUseCase {
    Mono<Boolean> putFareInCache(Fare fare);
}