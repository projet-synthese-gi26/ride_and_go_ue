package com.yowyob.rideandgo.domain.model;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record Vehicle(
    // ID du véhicule (Clé primaire)
    UUID id,

    // IDs des référentiels (Clés étrangères logiques)
    String vehicleMakeId,
    String vehicleModelId,
    String transmissionTypeId,
    String manufacturerId,
    String vehicleSizeId,
    String vehicleTypeId,
    String fuelTypeId,

    // Informations d'identification
    String vehicleSerialNumber,
    String vehicleSerialPhoto, // URL
    String registrationNumber, // Plaque d'immatriculation
    String registrationPhoto,  // URL

    // Capacités et caractéristiques
    int tankCapacity,
    int luggageMaxCapacity,
    int totalSeatNumber,
    double averageFuelConsumptionPerKm,
    int mileageAtStart,
    int mileageSinceCommissioning,
    int vehicleAgeAtStart,
    
    // Champ redondant mais présent
    String brand,

    List<String> illustrationImages
) {}