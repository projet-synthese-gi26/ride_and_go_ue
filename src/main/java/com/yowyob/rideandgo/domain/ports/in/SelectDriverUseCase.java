
// SelectDriverUseCase.java
package com.yowyob.rideandgo.domain.ports.in;
import com.yowyob.rideandgo.domain.model.Offer;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface SelectDriverUseCase {
    Mono<Offer> selectDriver(UUID offerId, UUID driverId);
}