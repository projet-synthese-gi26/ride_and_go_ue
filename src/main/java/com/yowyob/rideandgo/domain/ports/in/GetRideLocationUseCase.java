package com.yowyob.rideandgo.domain.ports.in;

import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.RideTrackingResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GetRideLocationUseCase {
    /**
     * Récupère la position, la distance et l'ETA du partenaire de course.
     */
    Mono<RideTrackingResponse> getPartnerLocation(UUID rideId, UUID requesterId);
}