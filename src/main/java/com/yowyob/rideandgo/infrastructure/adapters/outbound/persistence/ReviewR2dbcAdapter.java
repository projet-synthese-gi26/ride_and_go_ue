package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence;

import com.yowyob.rideandgo.domain.model.Review;
import com.yowyob.rideandgo.domain.ports.out.ReviewRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.ReviewEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.ReviewR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewR2dbcAdapter implements ReviewRepositoryPort {
    private final ReviewR2dbcRepository repository;

    @Override
    public Mono<Review> save(Review domain) {
        ReviewEntity entity = new ReviewEntity(
                domain.id(), domain.rideId(), domain.passengerId(),
                domain.driverId(), domain.rating(), domain.comment(),
                LocalDateTime.now(), true);
        return repository.save(entity).map(this::mapToDomain);
    }

    @Override
    public Mono<Double> getAverageRatingForDriver(UUID driverId) {
        return repository.getAverageRatingForDriver(driverId).defaultIfEmpty(0.0);
    }

    @Override
    public Mono<Long> countReviewsForDriver(UUID driverId) {
        return repository.countReviewsForDriver(driverId).defaultIfEmpty(0L);
    }

    @Override
    public Flux<Review> findAllByDriverId(UUID driverId) {
        return repository.findByDriverIdOrderByCreatedAtDesc(driverId)
                .map(this::mapToDomain);
    }

    private Review mapToDomain(ReviewEntity e) {
        return Review.builder()
                .id(e.getId()).rideId(e.getRideId()).passengerId(e.getPassengerId())
                .driverId(e.getDriverId()).rating(e.getRating()).comment(e.getComment())
                .createdAt(e.getCreatedAt()).build();
    }
}