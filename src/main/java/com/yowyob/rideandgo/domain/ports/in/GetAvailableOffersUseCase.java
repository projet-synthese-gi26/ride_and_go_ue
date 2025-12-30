// GetAvailableOffersUseCase.java
package com.yowyob.rideandgo.domain.ports.in;
import com.yowyob.rideandgo.domain.model.Offer;
import reactor.core.publisher.Flux;

public interface GetAvailableOffersUseCase {
    Flux<Offer> getAvailableOffers(); // Offers in PENDING state
}