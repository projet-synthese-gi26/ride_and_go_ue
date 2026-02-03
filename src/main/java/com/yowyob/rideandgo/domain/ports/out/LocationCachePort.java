package com.yowyob.rideandgo.domain.ports.out;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface LocationCachePort {
    /**
     * Sauvegarde la position en temps réel (Geo) ET l'ajoute à l'historique (List).
     */
    Mono<Boolean> saveLocation(UUID actorId, Double latitude, Double longitude);

    /**
     * Récupère la dernière position connue (Live).
     */
    Mono<Location> getLocation(UUID actorId);

    /**
     * Trouve les chauffeurs dans un rayon donné.
     * C'est ici que la magie Redis Geo opérera.
     */
    Flux<GeoResult> findNearbyDrivers(Double latitude, Double longitude, Double radiusKm);

    // Record simple pour les coordonnées
    record Location(Double latitude, Double longitude) {
    }

    // Record pour le résultat de recherche (ID + Distance)
    record GeoResult(UUID driverId, Double distanceKm, Location location) {
    }
}