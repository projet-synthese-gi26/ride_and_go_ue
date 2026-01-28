package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

public record BecomeDriverRequest(
    // Informations personnelles du chauffeur
    String licenseNumber,

    // Informations du véhicule (noms lisibles, pas IDs)
    // Le backend se chargera de les convertir en IDs
    String vehicleMakeName,       // ex: "Toyota"
    String vehicleModelName,      // ex: "Yaris"
    String vehicleTypeName,       // ex: "CAR"
    String transmissionTypeName,  // ex: "Manual"
    String fuelTypeName,          // ex: "Petrol"
    String vehicleSizeName,       // ex: "Standard"
    String manufacturerName,      // ex: "Toyota Motors"

    // Détails spécifiques du véhicule
    String vehicleSerialNumber,
    String registrationNumber, // Plaque
    int tankCapacity,
    int luggageMaxCapacity,
    int totalSeatNumber,
    double averageFuelConsumptionPerKm,
    int mileageAtStart,
    int mileageSinceCommissioning,
    int vehicleAgeAtStart
    // Les photos (vehicleSerialPhoto, registrationPhoto) seront gérées via un autre endpoint (multipart)
) {}