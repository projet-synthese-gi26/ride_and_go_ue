package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.Offer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface OfferRepositoryPort {
    Mono<Offer> save(Offer offer);
    Mono<Boolean> delete(Offer offer);
    Mono<Boolean> exists(Offer offer);
    Mono<Offer> findById(UUID offerId);
    Flux<Offer> findAll(); 
}