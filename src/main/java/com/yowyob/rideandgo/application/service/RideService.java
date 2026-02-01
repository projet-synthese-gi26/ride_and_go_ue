package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import com.yowyob.rideandgo.domain.ports.in.UpdateRideStatusUseCase;
import com.yowyob.rideandgo.domain.ports.out.RideRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
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
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Ride not found")))
                .flatMap(ride -> {
                    boolean isDriver = ride.driverId().equals(actorId);
                    boolean isPassenger = ride.passengerId().equals(actorId);

                    if (!isDriver && !isPassenger) {
                        return Mono.error(new IllegalStateException("Access Denied"));
                    }

                    if (isPassenger && (newStatus != RideState.CANCELLED || ride.state() != RideState.CREATED)) {
                        return Mono.error(new IllegalStateException("Passenger forbidden action"));
                    }

                    if (!isValidTransition(ride.state(), newStatus)) {
                        return Mono.error(new IllegalStateException("Invalid transition"));
                    }

                    Ride updatedRide = Ride.builder()
                            .id(ride.id()).offerId(ride.offerId()).passengerId(ride.passengerId())
                            .driverId(ride.driverId()).distance(ride.distance()).duration(ride.duration())
                            .state(newStatus).timeReal(ride.timeReal()).build();

                    return rideRepository.save(updatedRide);
                });
    }

    public Mono<Ride> getCurrentRideForDriver(UUID driverId) {
        return rideRepository.findCurrentRideByDriverId(driverId);
    }

    public Mono<Ride> getRideByOfferId(UUID offerId) {
        return rideRepository.findRideByOfferId(offerId);
    }

    public Mono<Ride> getRideById(UUID rideId) {
        return rideRepository.findRideById(rideId);
    }

    public Flux<Ride> getHistoryForUser(UUID userId, int page, int size) {
        return rideRepository.findRideHistoryByUserId(userId, page, size);
    }

    public Flux<Ride> getHistoryForDriver(UUID driverId, int page, int size) {
        return rideRepository.findRideHistoryByDriverId(driverId, page, size);
    }

    private boolean isValidTransition(RideState current, RideState target) {
        if (current == target)
            return true;
        return switch (current) {
            case CREATED -> target == RideState.ONGOING || target == RideState.CANCELLED;
            case ONGOING -> target == RideState.COMPLETED || target == RideState.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
