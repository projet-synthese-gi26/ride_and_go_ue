package com.yowyob.rideandgo.domain.model;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record DriverTrajectory(
        UUID id,
        UUID driverId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer pointsCount,
        String trajectoryDataJson // On le passe en String JSON pour l'adaptateur
) {
}