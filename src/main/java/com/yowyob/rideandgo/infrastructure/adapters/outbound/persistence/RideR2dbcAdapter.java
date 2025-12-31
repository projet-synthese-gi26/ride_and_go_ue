package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.ports.out.RideRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.RideEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.RideR2dbcRepository;
import com.yowyob.rideandgo.infrastructure.mappers.RideMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RideR2dbcAdapter implements RideRepositoryPort {

    private final RideR2dbcRepository repository;
    private final RideMapper mapper;

    /**
     * Sauvegarde une course (Ride).
     * Gère explicitement la distinction INSERT / UPDATE pour éviter l'erreur
     * "Row with Id does not exist" quand l'ID est pré-généré par le domaine.
     */
    @Override
    @Transactional
    public Mono<Ride> save(Ride ride) {
        RideEntity entity = mapper.toEntity(ride);

        // 1. On vérifie si la course existe déjà en base
        return repository.existsById(ride.id())
                .flatMap(exists -> {
                    if (!exists) {
                        // 2. Si elle n'existe pas, on force le flag 'newEntity' à true
                        // Cela indique à l'entité (qui implémente Persistable) de dire à Spring Data : "Fais un INSERT !"
                        entity.setNewEntity(true);
                    }
                    // 3. Sinon (exists = true), le flag reste false, Spring Data fera un UPDATE
                    return repository.save(entity);
                })
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Ride> findRideById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Ride> findCurrentRideByDriverId(UUID driverId) {
        return repository.findActiveRideByDriverId(driverId)
                .map(mapper::toDomain);
    }
}