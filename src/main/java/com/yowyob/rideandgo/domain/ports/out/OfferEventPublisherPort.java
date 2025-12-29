package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.Offer;
import reactor.core.publisher.Mono;

public interface OfferEventPublisherPort {
    Mono<Void> publishOfferCreatedEvent(Offer offer);
}
