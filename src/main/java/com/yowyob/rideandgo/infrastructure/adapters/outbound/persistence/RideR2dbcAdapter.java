package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.ports.out.RideRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.RideEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.RideR2dbcRepository;
import com.yowyob.rideandgo.infrastructure.mappers.RideMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideR2dbcAdapter implements RideRepositoryPort {

    private final RideR2dbcRepository repository;
    private final RideMapper mapper;

    @Override
    @Transactional
    public Mono<Ride> save(Ride ride) {
        RideEntity entity = mapper.toEntity(ride);

        // OPTIMISATION : Comme l'ID est généré par le domaine (Service), 
        // on sait que c'est une création. On force le flag 'newEntity' à true immédiatement.
        // Cela évite l'appel inutile à 'existsById' et garantit le retour de l'objet.
        if (ride.state() == com.yowyob.rideandgo.domain.model.enums.RideState.CREATED) {
             entity.setNewEntity(true); // Force INSERT SQL
        } else {
             entity.setNewEntity(false); // Force UPDATE SQL
        }

        return repository.save(entity)
                .map(mapper::toDomain)
                // Sécurité : si save retourne empty (ne devrait pas arriver sur un insert réussi), 
                // on log l'erreur pour comprendre.
                .switchIfEmpty(Mono.error(new RuntimeException("Erreur critique : La sauvegarde de la course a retourné vide.")));
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