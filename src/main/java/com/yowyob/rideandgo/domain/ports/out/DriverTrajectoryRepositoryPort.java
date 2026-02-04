package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.DriverTrajectory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface DriverTrajectoryRepositoryPort {
    Mono<Void> save(DriverTrajectory trajectory);

    // ✅ Nouvelle méthode pour récupérer l'historique
    Flux<DriverTrajectory> findAllByDriverId(UUID driverId);
}