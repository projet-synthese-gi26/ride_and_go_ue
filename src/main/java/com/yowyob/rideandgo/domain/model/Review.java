package com.yowyob.rideandgo.domain.model;

import lombok.Builder;
import java.util.UUID;
import java.time.LocalDateTime;

@Builder
public record Review(
    UUID id,
    UUID rideId,
    UUID passengerId,
    UUID driverId,
    int rating,
    String comment,
    LocalDateTime createdAt
) {}