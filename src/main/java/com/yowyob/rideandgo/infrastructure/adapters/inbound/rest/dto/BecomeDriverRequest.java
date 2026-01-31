package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO pour l'onboarding chauffeur.
 * Utilise les NOMS (ex: "Toyota") au lieu des IDs, conformément à l'API Vehicle
 * Simplified.
 * Les photos seront uploadées dans une étape ultérieure via l'API Vehicle.
 */
public record BecomeDriverRequest(
        String licenseNumber,
        @JsonProperty("vehicle") VehicleInfo vehicle) {
    public record VehicleInfo(
            // Identifiants textuels (Noms)
            String makeName, // ex: "Toyota"
            String modelName, // ex: "Corolla Hybride"
            String transmissionType, // ex: "Automatique"
            String manufacturerName, // ex: "Toyota Factory"
            String sizeName, // ex: "Berline Compacte"
            String typeName, // ex: "Personnel"
            String fuelTypeName, // ex: "Hybride Essence"
            
            // Données physiques
            String vehicleSerialNumber, // VIN
            String registrationNumber, // Plaque

            // Caractéristiques techniques
            int tankCapacity,
            int luggageMaxCapacity,
            int totalSeatNumber,
            double averageFuelConsumptionPerKm,
            int mileageAtStart,
            double mileageSinceCommissioning,
            double vehicleAgeAtStart) {
    }
}