package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import com.yowyob.rideandgo.domain.ports.in.UpdateRideStatusUseCase;
import com.yowyob.rideandgo.domain.ports.out.DriverRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.OfferRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.RideRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.UserRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.VehicleRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.EnrichedRideResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideService implements UpdateRideStatusUseCase {
    private final RideRepositoryPort rideRepository;
    private final OfferRepositoryPort offerRepository;
    private final UserRepositoryPort userRepository;
    private final DriverRepositoryPort driverRepository;
    private final VehicleRepositoryPort vehicleRepository;

    @Override
    @Transactional
    public Mono<Ride> updateRideStatus(UUID rideId, RideState newStatus, UUID actorId) {
        log.info("🔄 Transitioning Ride {} to state {}", rideId, newStatus);

        return rideRepository.findRideById(rideId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Course introuvable")))
                .flatMap(ride -> {
                    // 1. Vérification des droits (Chauffeur ou Passager)
                    boolean isDriver = ride.driverId().equals(actorId);
                    boolean isPassenger = ride.passengerId().equals(actorId);

                    if (!isDriver && !isPassenger) {
                        return Mono.error(
                                new IllegalStateException("Accès refusé : vous ne faites pas partie de cette course."));
                    }

                    // 2. Seul le chauffeur peut passer à ONGOING ou COMPLETED
                    if ((newStatus == RideState.ONGOING || newStatus == RideState.COMPLETED) && !isDriver) {
                        return Mono.error(
                                new IllegalStateException("Seul le chauffeur peut démarrer ou terminer la course."));
                    }

                    // 3. Validation de la transition d'état
                    if (!isValidTransition(ride.state(), newStatus)) {
                        return Mono.error(new IllegalStateException(
                                "Transition impossible de " + ride.state() + " vers " + newStatus));
                    }

                    // 4. Application du changement
                    Ride updatedRide = Ride.builder()
                            .id(ride.id())
                            .offerId(ride.offerId())
                            .passengerId(ride.passengerId())
                            .driverId(ride.driverId())
                            .distance(ride.distance())
                            .duration(ride.duration())
                            .state(newStatus) // ✅ NOUVEL ÉTAT
                            .timeReal(ride.timeReal())
                            .build();

                    return rideRepository.save(updatedRide)
                            .doOnSuccess(r -> log.info("✅ Ride {} is now {}", rideId, newStatus));
                });
    }

    public Flux<EnrichedRideResponse> getEnrichedHistoryForUser(UUID userId, int page, int size) {
        return rideRepository.findRideHistoryByUserId(userId, page, size)
                .flatMap(this::enrichRide);
    }

    public Flux<EnrichedRideResponse> getEnrichedHistoryForDriver(UUID driverId, int page, int size) {
        return rideRepository.findRideHistoryByDriverId(driverId, page, size)
                .flatMap(this::enrichRide);
    }

    private Mono<EnrichedRideResponse> enrichRide(Ride ride) {
        // 1. Infos Offre
        var offerMono = offerRepository.findById(ride.offerId()).defaultIfEmpty(null);
        
        // 2. Infos Chauffeur (User + Profile)
        var driverUserMono = userRepository.findUserById(ride.driverId()).defaultIfEmpty(null);
        var driverProfileMono = driverRepository.findById(ride.driverId()).defaultIfEmpty(null);
        
        // 3. Infos Passager
        var passengerUserMono = userRepository.findUserById(ride.passengerId()).defaultIfEmpty(null);

        return Mono.zip(offerMono, driverUserMono, driverProfileMono, passengerUserMono)
                .flatMap(tuple -> {
                    var offer = tuple.getT1();
                    var dUser = tuple.getT2();
                    var dProfile = tuple.getT3();
                    var pUser = tuple.getT4();

                    // 4. Infos Véhicule (dépend du profil chauffeur)
                    Mono<com.yowyob.rideandgo.domain.model.Vehicle> vehicleMono = Mono.empty();
                    if (dProfile != null && dProfile.vehicleId() != null) {
                        vehicleMono = vehicleRepository.getVehicleById(dProfile.vehicleId());
                    }

                    return vehicleMono.defaultIfEmpty(null)
                            .map(v -> EnrichedRideResponse.builder()
                                    .ride(ride)
                                    .startPoint(offer != null ? offer.startPoint() : "N/A")
                                    .endPoint(offer != null ? offer.endPoint() : "N/A")
                                    .price(offer != null ? offer.price() : 0.0)
                                    .driverName(dUser != null ? dUser.name() : "Inconnu")
                                    .driverPhoto(dUser != null ? dUser.photoUri() : null)
                                    .passengerName(pUser != null ? pUser.name() : "Inconnu")
                                    .passengerPhoto(pUser != null ? pUser.photoUri() : null)
                                    .vehicle(v)
                                    .build());
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
