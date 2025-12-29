package com.yowyob.rideandgo.domain.model;

import java.util.UUID;

public record Fare(
        UUID id,
        UUID userId,
        String startPoint,
        String endPoint,
        Double estimatedFare,
        Double  officialFare
) {
}
