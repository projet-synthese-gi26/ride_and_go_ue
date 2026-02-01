package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.application.utils.Utils;
import com.yowyob.rideandgo.domain.model.Review;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import com.yowyob.rideandgo.domain.ports.out.DriverRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.ReviewRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.RideRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepositoryPort reviewRepository;
    private final RideRepositoryPort rideRepository;
    private final DriverRepositoryPort driverRepository;

    @Transactional
    public Mono<Review> submitReview(UUID rideId, UUID passengerId, int stars, String comment) {
        log.info("⭐ Submitting review for ride {}: {} stars", rideId, stars);

        return rideRepository.findRideById(rideId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Course introuvable")))
                .flatMap(ride -> {
                    if (ride.state() != RideState.COMPLETED) {
                        return Mono.error(new IllegalStateException("La course doit être terminée pour être notée."));
                    }
                    if (!ride.passengerId().equals(passengerId)) {
                        return Mono.error(new IllegalStateException("Seul le passager de cette course peut la noter."));
                    }

                    Review review = Review.builder()
                            .id(Utils.generateUUID())
                            .rideId(rideId)
                            .driverId(ride.driverId())
                            .passengerId(passengerId)
                            .rating(stars)
                            .comment(comment)
                            .build();

                    return reviewRepository.save(review)
                            .flatMap(saved -> updateDriverStats(ride.driverId()).thenReturn(saved));
                });
    }

    private Mono<Void> updateDriverStats(UUID driverId) {
        return Mono.zip(
                reviewRepository.getAverageRatingForDriver(driverId),
                reviewRepository.countReviewsForDriver(driverId)
        ).flatMap(tuple -> driverRepository.findById(driverId)
                .flatMap(driver -> {
                    var updatedDriver = driver.toBuilder()
                            .rating(tuple.getT1())
                            .totalReviewsCount(tuple.getT2().intValue())
                            .build();
                    return driverRepository.save(updatedDriver);
                })).then();
    }
}