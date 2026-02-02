package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.ReviewEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface ReviewR2dbcRepository extends ReactiveCrudRepository<ReviewEntity, UUID> {
    
    Flux<ReviewEntity> findByDriverIdOrderByCreatedAtDesc(UUID driverId);

    // ✅ Utilisation de COALESCE et CAST pour garantir un Double 0.0 (jamais NULL)
    @Query("SELECT CAST(COALESCE(AVG(rating), 0.0) AS DOUBLE PRECISION) FROM reviews WHERE driver_id = :driverId")
    Mono<Double> getAverageRatingForDriver(UUID driverId);

    @Query("SELECT COUNT(*) FROM reviews WHERE driver_id = :driverId")
    Mono<Long> countReviewsForDriver(UUID driverId);
}