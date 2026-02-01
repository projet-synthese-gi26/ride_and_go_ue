package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.Ride;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface RideRepositoryPort {
    Mono<Ride> save(Ride ride);

    Mono<Ride> findRideById(UUID id);

    Mono<Ride> findCurrentRideByDriverId(UUID driverId);

    Mono<Ride> findRideByOfferId(UUID offerId);

    Mono<Long> countCompletedRidesByDriverId(UUID driverId);

    Flux<Ride> findRideHistoryByUserId(UUID userId, int page, int size);

    Flux<Ride> findRideHistoryByDriverId(UUID driverId, int page, int size);
}
