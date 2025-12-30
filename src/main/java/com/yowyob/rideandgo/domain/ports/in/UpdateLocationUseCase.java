package com.yowyob.rideandgo.domain.ports.in;

import reactor.core.publisher.Mono;

/**
 * Input port for real-time location updates.
 */
public interface UpdateLocationUseCase {
    /**
     * Updates the GPS coordinates of the current authenticated actor.
     * @param latitude Double
     * @param longitude Double
     * @return Mono<Boolean> true if success
     */
    Mono<Boolean> updateCurrentLocation(Double latitude, Double longitude);
}