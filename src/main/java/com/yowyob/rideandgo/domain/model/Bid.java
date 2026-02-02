package com.yowyob.rideandgo.domain.model;

import lombok.Builder;
import java.util.List;
import java.util.UUID;

@Builder
public record Bid(
        UUID driverId,
        String driverName,
        String driverPhone, // ✅ Ajouté
        String driverPhoto,
        Double rating,
        Integer totalTrips,
        Integer eta,
        Double distanceToPassenger,
        Double latitude,
        Double longitude,
        String vehicleId,
        String brand,
        String model,
        String color,
        String licensePlate,
        String vehicleType,
        Integer manufacturingYear,
        List<String> vehicleImages) {
}