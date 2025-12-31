package com.yowyob.rideandgo.domain.ports.in;

import com.yowyob.rideandgo.domain.model.Offer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface OfferManagementUseCase {
    Flux<Offer> getAllOffers();
    Mono<Offer> getOfferById(UUID id);
    Mono<Offer> updateOffer(UUID id, Offer offerDetails);
    Mono<Boolean> deleteOffer(UUID id);
}