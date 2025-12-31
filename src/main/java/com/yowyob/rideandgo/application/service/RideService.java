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
                    boolean isDriver = ride.driverId().equals(actorId);
                    boolean isPassenger = ride.passengerId().equals(actorId);

                    // 1. Vérification de Sécurité (Qui es-tu ?)
                    if (!isDriver && !isPassenger) {
                        return Mono.error(new IllegalStateException("Access Denied: You are not involved in this ride."));
                    }

                    // 2. Règles Métier pour le PASSAGER
                    if (isPassenger) {
                        // Le passager ne peut QUE annuler
                        if (newStatus != RideState.CANCELLED) {
                            return Mono.error(new IllegalStateException("Passenger is not allowed to change status to " + newStatus));
                        }
                        // Le passager ne peut annuler QUE si la course n'a pas démarré
                        if (ride.state() != RideState.CREATED) {
                            return Mono.error(new IllegalStateException("Too late to cancel. The ride has already started or finished."));
                        }
                    }

                    // 3. Règles Métier pour la Machine à États (Valable pour Driver & Passenger)
                    if (!isValidTransition(ride.state(), newStatus)) {
                        return Mono.error(new IllegalStateException(
                                String.format("Invalid transition from %s to %s", ride.state(), newStatus)));
                    }

                    // 4. Application de la mise à jour
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
                .doOnSuccess(r -> log.info("Ride {} status updated to {} by actor {}", r.id(), r.state(), actorId));
    }

    public Mono<Ride> getCurrentRideForDriver(UUID driverId) {
        return rideRepository.findCurrentRideByDriverId(driverId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No active ride found for this driver")));
    }

    private boolean isValidTransition(RideState current, RideState target) {
        if (current == target) return true;
        return switch (current) {
            case CREATED -> target == RideState.ONGOING || target == RideState.CANCELLED;
            case ONGOING -> target == RideState.COMPLETED || target == RideState.CANCELLED;
            case COMPLETED, CANCELLED -> false; // États finaux
        };
    }
}