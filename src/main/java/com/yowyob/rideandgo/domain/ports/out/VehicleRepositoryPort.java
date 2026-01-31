package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.Vehicle;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

// Gère le cache Redis et l'appel distant
public interface VehicleRepositoryPort {
    // Création via la route simplifiée
    Mono<Vehicle> createVehicle(Vehicle vehicle);

    // Récupération (Cache -> Distant)
    Mono<Vehicle> getVehicleById(UUID vehicleId);

    // Mise à jour partielle (Patch)
    Mono<Vehicle> patchVehicle(UUID vehicleId, Vehicle partialUpdate);

    // Gestion des Documents Obligatoires
    Mono<Vehicle> uploadRegistrationDocument(UUID vehicleId, FilePart file);

    Mono<Vehicle> uploadSerialDocument(UUID vehicleId, FilePart file);

    // Gestion de la Galerie d'images
    Mono<String> uploadVehicleImage(UUID vehicleId, FilePart file); // Retourne l'URL/Path

    Flux<String> getVehicleImages(UUID vehicleId); // Retourne les URLs

    Mono<Void> cacheVehicle(Vehicle vehicle);
}