package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.Fare;
import com.yowyob.rideandgo.domain.ports.in.PutFareInCacheUseCase;
import com.yowyob.rideandgo.domain.ports.out.FareCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class FareService implements PutFareInCacheUseCase {

    private final FareCachePort fareCachePort;

    @Override
    public Mono<Boolean> putFareInCache(Fare fare) {
        return fareCachePort.saveInCache(fare);
    }
}