package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

public record UpdateVehicleDto(
        String makeName,
        String modelName,
        String transmissionType,
        String manufacturerName,
        String sizeName,
        String typeName,
        String fuelTypeName,
        String vehicleSerialNumber,
        String registrationNumber,
        Integer tankCapacity,
        Integer luggageMaxCapacity,
        Integer totalSeatNumber,
        Double averageFuelConsumptionPerKm,
        Integer mileageAtStart,
        Integer mileageSinceCommissioning,
        Integer vehicleAgeAtStart) {
}