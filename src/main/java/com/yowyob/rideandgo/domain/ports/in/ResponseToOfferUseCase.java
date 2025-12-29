package com.yowyob.rideandgo.domain.ports.in;

import com.yowyob.rideandgo.domain.model.Offer;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ResponseToOfferUseCase {
    Mono<Offer> responseToOffer(UUID offerId, UUID driverId);
}