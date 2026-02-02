package com.yowyob.rideandgo.domain.model;

import com.yowyob.rideandgo.domain.model.enums.RideState;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record Ride(
        UUID id,
        UUID offerId,
        UUID passengerId,
        UUID driverId,
        double distance,
        int duration,
        RideState state,
        int timeReal,
        LocalDateTime createdAt // ✅ Ajouté
) {}