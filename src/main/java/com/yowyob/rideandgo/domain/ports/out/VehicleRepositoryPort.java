package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.Vehicle;
import reactor.core.publisher.Mono;
import java.util.UUID;

// Gère le cache Redis et l'appel distant (Pattern Proxy/Cache-Aside)
public interface VehicleRepositoryPort {
    Mono<Vehicle> createVehicle(Vehicle vehicle); // Appelle Service Distant

    Mono<Vehicle> getVehicleById(UUID vehicleId); // Cherche Cache -> Sinon Distant -> Met en Cache

    Mono<Void> cacheVehicle(Vehicle vehicle);
}