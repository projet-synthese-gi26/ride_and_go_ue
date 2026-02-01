package com.yowyob.rideandgo.domain.ports.out;

import com.yowyob.rideandgo.domain.model.Review;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface ReviewRepositoryPort {
    Mono<Review> save(Review review);

    Mono<Double> getAverageRatingForDriver(UUID driverId);

    Mono<Long> countReviewsForDriver(UUID driverId);
}