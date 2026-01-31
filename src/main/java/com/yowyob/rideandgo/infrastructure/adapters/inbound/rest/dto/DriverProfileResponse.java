package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.Vehicle;
import java.util.UUID;

public record DriverProfileResponse(
        UUID userId,
        String status,
        String licenseNumber,
        boolean isOnline,
        boolean isProfileValidated,
        Vehicle vehicle) {
}