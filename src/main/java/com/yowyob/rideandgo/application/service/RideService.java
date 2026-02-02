package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.Vehicle;
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

    public Flux<EnrichedRideResponse> getEnrichedHistory(UUID userId, int page, int size) {
        return rideRepository.findRideHistoryByUserId(userId, page, size)
                .flatMap(ride -> enrichRide(ride, userId));
    }

    private Mono<EnrichedRideResponse> enrichRide(Ride ride, UUID requesterId) {
        // 1. Déterminer qui est le partenaire (l'autre personne)
        UUID partnerId = ride.driverId().equals(requesterId) ? ride.passengerId() : ride.driverId();

        // 2. Appel Partenaire (Robuste)
        Mono<User> partnerMono = userRepository.findUserById(partnerId)
                .onErrorResume(e -> Mono.empty())
                .defaultIfEmpty(User.builder().name("Inconnu").firstName("Utilisateur").lastName("Inconnu").build());

        // 3. Appel Offre (pour récupérer points de départ/arrivée et prix)
        Mono<Offer> offerMono = offerRepository.findById(ride.offerId())
                .onErrorResume(e -> Mono.empty())
                .defaultIfEmpty(Offer.builder().startPoint("N/A").endPoint("N/A").price(0.0).build());

        // 4. Appel Véhicule (Uniquement si le demandeur est le passager)
        Mono<Vehicle> vehicleMono = Mono.empty();
        if (requesterId.equals(ride.passengerId())) {
            vehicleMono = driverRepository.findById(ride.driverId())
                    .flatMap(d -> d.vehicleId() != null ? vehicleRepository.getVehicleById(d.vehicleId()) : Mono.empty())
                    .onErrorResume(e -> Mono.empty());
        }

        // 5. Agrégation avec ZIP (Toutes les branches sont sécurisées)
        return Mono.zip(partnerMono, offerMono, vehicleMono.defaultIfEmpty(Vehicle.builder().brand("N/A").build()))
                .map(tuple -> {
                    User partner = tuple.getT1();
                    Offer offer = tuple.getT2();
                    Vehicle vehicle = tuple.getT3();

                    return EnrichedRideResponse.builder()
                            .rideId(ride.id())
                            .state(ride.state())
                            .distance(ride.distance())
                            .price(offer.price())
                            .startPoint(offer.startPoint())
                            .endPoint(offer.endPoint())
                            .partnerName(partner.firstName() + " " + partner.lastName())
                            .partnerPhone(partner.telephone())
                            .partnerPhoto(partner.photoUri())
                            .createdAt(ride.createdAt())
                            .vehicle(vehicle.id() != null ? vehicle : null)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("CRITICAL: Failed to enrich ride {}, skipping item", ride.id(), e);
                    return Mono.empty(); // En dernier recours, on ignore cette course plutôt que de tout faire planter
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
