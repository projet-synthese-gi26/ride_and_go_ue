package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.RideEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RideR2dbcRepository extends ReactiveCrudRepository<RideEntity, UUID> {
    // On cherche une course qui est soit CREATED (en approche), soit ONGOING (en cours)
    @Query("SELECT * FROM rides WHERE driver_id = :driverId AND state IN ('CREATED', 'ONGOING') LIMIT 1")
    Mono<RideEntity> findActiveRideByDriverId(UUID driverId);

    // --- NOUVEAU ---
    Mono<RideEntity> findByOfferId(UUID offerId);
}