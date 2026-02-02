package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private UUID reviewId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    // Infos sur celui qui a laissé l'avis
    private String passengerName;
    private String passengerPhoto;
}