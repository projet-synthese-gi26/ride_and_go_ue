package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.model.enums.RideState;
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
        log.info("💾 Saving Ride: ID={} State={}", ride.id(), ride.state());
        RideEntity entity = mapper.toEntity(ride);

        // Forçage de l'INSERT si c'est une création
        if (ride.state() == RideState.CREATED) {
             entity.setNewEntity(true); 
             log.debug("👉 Force INSERT for Ride {}", ride.id());
        } else {
             entity.setNewEntity(false);
             log.debug("👉 Force UPDATE for Ride {}", ride.id());
        }

        return repository.save(entity)
                .doOnSuccess(e -> {
                    if (e == null) log.error("❌ DB returned NULL for ride {}", ride.id());
                    else log.info("✅ DB saved ride {}", e.getId());
                })
                .map(savedEntity -> {
                    Ride domain = mapper.toDomain(savedEntity);
                    if (domain == null) throw new IllegalStateException("Mapping Entity -> Domain returned null!");
                    return domain;
                })
                // CORRECTION CRITIQUE : Si le save retourne vide (ce qui ne devrait pas arriver sur un insert réussi),
                // on renvoie l'objet qu'on a tenté de sauvegarder pour ne pas briser la chaîne,
                // ou on lève une erreur explicite.
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("❌ CRITICAL: Repository returned empty for Ride {}. Falling back to input object.", ride.id());
                    // Fallback : On suppose que ça a marché si pas d'exception, on renvoie l'objet entrant
                    return Mono.just(ride); 
                }));
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
    
    @Override
    public Mono<Ride> findRideByOfferId(UUID offerId) {
        return repository.findByOfferId(offerId)
                .map(mapper::toDomain);
    }
}