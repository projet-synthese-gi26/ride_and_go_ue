package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO structuré pour la demande "Devenir Chauffeur".
 * Regroupe les infos du véhicule sous une clé "vehicle".
 */
public record BecomeDriverRequest(
        String licenseNumber,
        @JsonProperty("vehicle") VehicleInfo vehicle) {
    public record VehicleInfo(
            // Informations du véhicule (noms lisibles, pas IDs)
            String vehicleMakeName, // ex: "Toyota"
            String vehicleModelName, // ex: "Yaris"
            String vehicleTypeName, // ex: "CAR"
            String transmissionTypeName, // ex: "Manual"
            String fuelTypeName, // ex: "Petrol"
            String vehicleSizeName, // ex: "Standard"
            String manufacturerName, // ex: "Toyota Motors"

            // Détails spécifiques du véhicule
            String vehicleSerialNumber,
            String registrationNumber, // Plaque
            int tankCapacity,
            int luggageMaxCapacity,
            int totalSeatNumber,
            double averageFuelConsumptionPerKm,
            int mileageAtStart,
            int mileageSinceCommissioning,
            int vehicleAgeAtStart) {
    }
}
