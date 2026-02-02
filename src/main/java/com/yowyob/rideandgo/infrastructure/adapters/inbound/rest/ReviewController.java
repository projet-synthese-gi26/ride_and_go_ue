package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.application.service.ReviewService;
import com.yowyob.rideandgo.domain.model.Review;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Review-Controller")
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/ride/{rideId}")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_PASSENGER')")
    public Mono<Review> postReview(@PathVariable UUID rideId, @RequestBody ReviewRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> UUID.fromString(ctx.getAuthentication().getName()))
                .flatMap(passengerId -> reviewService.submitReview(rideId, passengerId, request.stars(),
                        request.comment()));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my reviews (for drivers)")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_DRIVER')")
    public Flux<ReviewResponse> getMyReviews() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> UUID.fromString(ctx.getAuthentication().getName()))
                .flatMapMany(reviewService::getReviewsForDriver);
    }

    public record ReviewRequest(int stars, String comment) {
    }
}