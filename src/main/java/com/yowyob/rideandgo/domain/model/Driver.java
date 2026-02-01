package com.yowyob.rideandgo.domain.model;

import lombok.Builder;
import java.util.UUID;

@Builder(toBuilder = true)
public record Driver(
        UUID id,
        String status,
        String licenseNumber,
        boolean hasCar,
        boolean isOnline,
        boolean isProfileCompleted,
        boolean isProfileValidated,
        boolean isSyndicated,
        UUID vehicleId,
        Double rating, // NOUVEAU
        Integer totalReviewsCount // NOUVEAU
) {
}