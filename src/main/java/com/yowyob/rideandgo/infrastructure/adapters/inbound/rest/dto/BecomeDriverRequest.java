package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BecomeDriverRequest(
                String licenseNumber,
                @JsonProperty("vehicle") VehicleInfo vehicle) {
        public record VehicleInfo(
                        String makeName,
                        String modelName,
                        String transmissionType,
                        String manufacturerName,
                        String sizeName,
                        String typeName,
                        String fuelTypeName,
                        String vehicleSerialNumber,
                        String registrationNumber,

                        double tankCapacity,
                        double luggageMaxCapacity,
                        int totalSeatNumber,
                        double averageFuelConsumptionPerKm,
                        double mileageAtStart,
                        double mileageSinceCommissioning,
                        double vehicleAgeAtStart,

                        boolean airConditioned,
                        boolean comfortable,
                        boolean soft,
                        boolean screen,
                        boolean wifi,
                        boolean tollCharge,
                        boolean carParking,
                        boolean alarm,
                        boolean stateTax,
                        boolean driverAllowance,
                        boolean pickupAndDrop,
                        boolean internet,
                        boolean petsAllow) {
        }
}