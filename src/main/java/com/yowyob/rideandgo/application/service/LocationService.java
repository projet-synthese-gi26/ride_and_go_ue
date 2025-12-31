package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.ports.in.GetRideLocationUseCase;
import com.yowyob.rideandgo.domain.ports.in.UpdateLocationUseCase;
import com.yowyob.rideandgo.domain.ports.out.LocationCachePort;
import com.yowyob.rideandgo.domain.ports.out.RideRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.RideTrackingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService implements UpdateLocationUseCase, GetRideLocationUseCase {

    private final LocationCachePort locationCachePort;
    private final RideRepositoryPort rideRepositoryPort;
    private final TrackingCalculatorService trackingCalculatorService; // Injection du nouveau service

    // --- 1. Mise à jour de ma position (Existante) ---
    @Override
    public Mono<Boolean> updateCurrentLocation(Double latitude, Double longitude) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    String userIdStr = auth.getName(); 
                    try {
                        UUID userId = UUID.fromString(userIdStr);
                        return locationCachePort.saveLocation(userId, latitude, longitude);
                    } catch (IllegalArgumentException e) {
                        log.error("Security Context principal is not a valid UUID: {}", userIdStr);
                        return Mono.just(false);
                    }
                })
                .switchIfEmpty(Mono.error(new RuntimeException("No security context found for location update")));
    }

    // --- 2. Consultation Intelligente (Nouvelle Implémentation) ---
    @Override
    public Mono<RideTrackingResponse> getPartnerLocation(UUID rideId, UUID requesterId) {
        return rideRepositoryPort.findRideById(rideId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Ride not found")))
                .flatMap(ride -> {
                    UUID targetId;
                    String targetRole;

                    // Détermination Symétrique
                    if (requesterId.equals(ride.driverId())) {
                        targetId = ride.passengerId();
                        targetRole = "PASSENGER";
                    } else if (requesterId.equals(ride.passengerId())) {
                        targetId = ride.driverId();
                        targetRole = "DRIVER";
                    } else {
                        return Mono.error(new IllegalStateException("Access Denied: You are not part of this ride"));
                    }

                    // Récupération Parallèle des 2 positions
                    Mono<LocationCachePort.Location> targetLocMono = locationCachePort.getLocation(targetId)
                            .defaultIfEmpty(new LocationCachePort.Location(0.0, 0.0));
                    
                    Mono<LocationCachePort.Location> myLocMono = locationCachePort.getLocation(requesterId)
                            .defaultIfEmpty(new LocationCachePort.Location(0.0, 0.0));

                    return Mono.zip(targetLocMono, myLocMono)
                            .map(tuple -> {
                                LocationCachePort.Location targetLoc = tuple.getT1();
                                LocationCachePort.Location myLoc = tuple.getT2();

                                double distance = 0.0;
                                int eta = 0;

                                // On ne calcule que si les positions sont valides
                                if (targetLoc.latitude() != 0.0 && myLoc.latitude() != 0.0) {
                                    distance = trackingCalculatorService.calculateDistance(
                                            myLoc.latitude(), myLoc.longitude(),
                                            targetLoc.latitude(), targetLoc.longitude()
                                    );
                                    eta = trackingCalculatorService.calculateEtaInMinutes(distance);
                                }

                                return new RideTrackingResponse(
                                        targetLoc.latitude(),
                                        targetLoc.longitude(),
                                        distance,
                                        eta,
                                        targetRole
                                );
                            });
                });
    }
}