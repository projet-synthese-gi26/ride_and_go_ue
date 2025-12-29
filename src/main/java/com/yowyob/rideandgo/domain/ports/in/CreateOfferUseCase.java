package com.yowyob.rideandgo.domain.ports.in;

import com.yowyob.rideandgo.domain.model.Fare;
import com.yowyob.rideandgo.domain.model.Offer;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CreateOfferUseCase {
    Mono<Offer> createOffer(Offer offer, UUID passengerId);
}