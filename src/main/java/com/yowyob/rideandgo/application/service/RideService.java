package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import com.yowyob.rideandgo.domain.ports.in.UpdateRideStatusUseCase;
import com.yowyob.rideandgo.domain.ports.out.RideRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideService implements UpdateRideStatusUseCase {

    private final RideRepositoryPort rideRepository;

    @Override
    public Mono<Ride> updateRideStatus(UUID rideId, RideState newStatus, UUID actorId) {
        return rideRepository.findRideById(rideId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Ride not found with id: " + rideId)))
                .flatMap(ride -> {
                    // 1. Security Check
                    if (!ride.driverId().equals(actorId)) {
                        return Mono.error(new IllegalStateException("Access Denied: You are not the driver of this ride."));
                    }

                    // 2. State Machine Logic
                    if (!isValidTransition(ride.state(), newStatus)) {
                        return Mono.error(new IllegalStateException(
                                String.format("Invalid transition from %s to %s", ride.state(), newStatus)));
                    }

                    // 3. Apply Update
                    Ride updatedRide = new Ride(
                            ride.id(),
                            ride.offerId(),
                            ride.passengerId(),
                            ride.driverId(),
                            ride.distance(),
                            ride.duration(),
                            newStatus, 
                            ride.timeReal()
                    );

                    return rideRepository.save(updatedRide);
                })
                .doOnSuccess(r -> log.info("Ride {} status updated to {}", r.id(), r.state()));
    }

    /**
     * Nouvelle méthode pour le scénario de test E2E (Chauffeur récupère sa course)
     */
    public Mono<Ride> getCurrentRideForDriver(UUID driverId) {
        return rideRepository.findCurrentRideByDriverId(driverId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No active ride found for this driver")));
    }

    private boolean isValidTransition(RideState current, RideState target) {
        if (current == target) return true;
        return switch (current) {
            case CREATED -> target == RideState.ONGOING || target == RideState.CANCELLED;
            case ONGOING -> target == RideState.COMPLETED || target == RideState.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}