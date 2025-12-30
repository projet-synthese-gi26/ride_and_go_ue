package com.yowyob.rideandgo.infrastructure.adapters.outbound.external;

import com.yowyob.rideandgo.domain.ports.out.FareClientPort;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FareResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class FakeFareAdapter implements FareClientPort {

    private static final double MINIMUM_CAMEROON_FARE = 350.0;

    @Override
    public Mono<FareResponse> caclculateFare(FareRequest request) {
        log.info("🛠 MODE FAKE FARE : Calculation for {} to {}", request.startLocationName(), request.endLocationName());
        
        // Simulating random distance between 1km and 10km
        double distanceKm = ThreadLocalRandom.current().nextDouble(1.0, 10.0);
        
        // Logic: 250 base + 150 per km
        double calculatedFare = 250.0 + (distanceKm * 150.0);
        
        // Apply Cameroon floor price (350 XAF)
        double officialFare = Math.max(calculatedFare, MINIMUM_CAMEROON_FARE);
        
        // Estimate is slightly lower for the passenger proposal
        double estimatedFare = Math.max(officialFare * 0.9, MINIMUM_CAMEROON_FARE);

        return Mono.just(new FareResponse(
                estimatedFare,
                officialFare,
                request.startLocationName(),
                request.endLocationName(),
                false // Dynamic calculation, not from cache
        ));
    }
}