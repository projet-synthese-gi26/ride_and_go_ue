package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.Vehicle;
import com.yowyob.rideandgo.domain.ports.out.VehicleRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {
    private final VehicleRepositoryPort repository;

    /**
     * Flux complet de création :
     * 1. Crée le véhicule (données JSON)
     * 2. Upload la photo d'immatriculation (si présente)
     * 3. Upload la photo du numéro de série (si présente)
     */
    public Mono<Vehicle> createVehicleWithDocuments(Vehicle vehicleData, FilePart regPhoto, FilePart serialPhoto) {
        return repository.createVehicle(vehicleData)
                .flatMap(created -> {
                    Mono<Vehicle> chain = Mono.just(created);

                    if (regPhoto != null) {
                        chain = chain.flatMap(v -> repository.uploadRegistrationDocument(v.id(), regPhoto)
                                .doOnSuccess(x -> log.info("✅ Registration photo uploaded for {}", v.id())));
                    }

                    if (serialPhoto != null) {
                        chain = chain.flatMap(v -> repository.uploadSerialDocument(v.id(), serialPhoto)
                                .doOnSuccess(x -> log.info("✅ Serial photo uploaded for {}", v.id())));
                    }

                    return chain;
                });
    }

    public Mono<Vehicle> getVehicleById(UUID id) {
        return repository.getVehicleById(id);
    }

    public Mono<Vehicle> patchVehicle(UUID id, Vehicle partial) {
        return repository.patchVehicle(id, partial);
    }

    public Mono<String> addImage(UUID id, FilePart image) {
        return repository.uploadVehicleImage(id, image);
    }

    public Flux<String> getImages(UUID id) {
        return repository.getVehicleImages(id);
    }
}