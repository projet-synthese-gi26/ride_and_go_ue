package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.RideEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface RideR2dbcRepository extends ReactiveCrudRepository<RideEntity, UUID> {
    @Query("SELECT * FROM rides WHERE driver_id = :driverId AND state IN ('CREATED', 'ONGOING') LIMIT 1")
    Mono<RideEntity> findActiveRideByDriverId(UUID driverId);

    Mono<RideEntity> findByOfferId(UUID offerId);

    @Query("SELECT COUNT(*) FROM rides WHERE driver_id = :driverId AND state = 'COMPLETED'")
    Mono<Long> countCompletedByDriverId(UUID driverId);

    @Query("SELECT * FROM rides WHERE (driver_id = :userId OR passenger_id = :userId) ORDER BY created_at DESC OFFSET :offset LIMIT :limit")
    Flux<RideEntity> findHistoryByUserId(UUID userId, long offset, int limit);

    @Query("SELECT * FROM rides WHERE driver_id = :driverId ORDER BY created_at DESC OFFSET :offset LIMIT :limit")
    Flux<RideEntity> findHistoryByDriverId(UUID driverId, long offset, int limit);
}