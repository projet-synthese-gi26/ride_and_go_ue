package com.yowyob.rideandgo.domain.ports.in;

import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UpdateRideStatusUseCase {
    /**
     * Updates the status of a ride obeying business rules.
     * @param rideId The ID of the ride
     * @param newStatus The target status (ONGOING, COMPLETED, CANCELLED)
     * @param actorId The ID of the user performing the action (Security)
     * @return The updated Ride
     */
    Mono<Ride> updateRideStatus(UUID rideId, RideState newStatus, UUID actorId);
}