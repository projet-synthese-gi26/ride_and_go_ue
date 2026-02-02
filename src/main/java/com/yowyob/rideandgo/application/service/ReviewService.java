package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.application.utils.Utils;
import com.yowyob.rideandgo.domain.model.Review;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.enums.RideState;
import com.yowyob.rideandgo.domain.ports.out.DriverRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.ReviewRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.RideRepositoryPort;
import com.yowyob.rideandgo.domain.ports.out.UserRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepositoryPort reviewRepository;
    private final RideRepositoryPort rideRepository;
    private final DriverRepositoryPort driverRepository;
    private final UserRepositoryPort userRepository;

    @Transactional
    public Mono<Review> submitReview(UUID rideId, UUID passengerId, int stars, String comment) {
        return rideRepository.findRideById(rideId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Course introuvable")))
                .flatMap(ride -> {
                    if (ride.state() != RideState.COMPLETED) {
                        return Mono.error(new IllegalStateException("La course doit être terminée pour être notée."));
                    }
                    if (!ride.passengerId().equals(passengerId)) {
                        return Mono.error(new IllegalStateException("Seul le passager peut noter cette course."));
                    }
                    Review review = Review.builder()
                            .id(Utils.generateUUID()).rideId(rideId).driverId(ride.driverId())
                            .passengerId(passengerId).rating(stars).comment(comment).build();

                    return reviewRepository.save(review)
                            .flatMap(saved -> updateDriverStats(ride.driverId()).thenReturn(saved));
                });
    }

    public Flux<ReviewResponse> getReviewsForDriver(UUID driverId) {
        return reviewRepository.findAllByDriverId(driverId)
                .flatMap(review -> userRepository.findUserById(review.passengerId())
                        .map(user -> mapToResponse(review, user))
                        // Fallback si l'utilisateur n'est pas trouvé
                        .defaultIfEmpty(mapToResponse(review,
                                User.builder().firstName("Passager").lastName("Anonyme").build())));
    }

    private ReviewResponse mapToResponse(Review review, User passenger) {
        return ReviewResponse.builder()
                .reviewId(review.id())
                .rating(review.rating())
                .comment(review.comment())
                .createdAt(review.createdAt())
                .passengerName(passenger.firstName() + " " + passenger.lastName())
                .passengerPhoto(passenger.photoUri())
                .build();
    }

    private Mono<Void> updateDriverStats(UUID driverId) {
        return Mono.zip(
                reviewRepository.getAverageRatingForDriver(driverId),
                reviewRepository.countReviewsForDriver(driverId)).flatMap(
                        tuple -> driverRepository.findById(driverId)
                                .flatMap(driver -> driverRepository.save(driver.toBuilder()
                                        .rating(tuple.getT1())
                                        .totalReviewsCount(tuple.getT2().intValue())
                                        .build())))
                .then();
    }
}