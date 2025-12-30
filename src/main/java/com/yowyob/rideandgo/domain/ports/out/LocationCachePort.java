package com.yowyob.rideandgo.domain.ports.out;

import reactor.core.publisher.Mono;
import java.util.UUID;

public interface LocationCachePort {
    /**
     * Saves the live location of an actor (Driver or Passenger).
     * @param actorId The ID of the BusinessActor
     * @param latitude GPS Latitude
     * @param longitude GPS Longitude
     */
    Mono<Boolean> saveLocation(UUID actorId, Double latitude, Double longitude);

    /**
     * Retrieves the last known location from cache.
     */
    Mono<Location> getLocation(UUID actorId);

    /**
     * Internal domain record for coordinates
     */
    record Location(Double latitude, Double longitude) {}
}