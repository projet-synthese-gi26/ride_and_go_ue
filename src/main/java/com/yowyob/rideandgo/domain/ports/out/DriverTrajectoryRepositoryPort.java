package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.DriverTrajectory;
import reactor.core.publisher.Mono;

public interface DriverTrajectoryRepositoryPort {
    Mono<Void> save(DriverTrajectory trajectory);
}