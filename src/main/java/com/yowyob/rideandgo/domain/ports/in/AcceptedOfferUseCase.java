package com.yowyob.rideandgo.domain.ports.in;

import com.yowyob.rideandgo.domain.model.Ride;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AcceptedOfferUseCase {
    Mono<Ride> acceptedOffer(UUID offerId, UUID passengerId, UUID driverId);
}
