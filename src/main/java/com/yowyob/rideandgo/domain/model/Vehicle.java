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

    // Capacités et caractéristiques (Alignés avec les doubles de l'API)
    double tankCapacity,
    double luggageMaxCapacity,
    int totalSeatNumber,
    double averageFuelConsumptionPerKm,
    double mileageAtStart,
    double mileageSinceCommissioning,
    double vehicleAgeAtStart,
    
    // Champ redondant mais présent
    String brand,

    List<String> illustrationImages,

    // --- NOUVEAUX CHAMPS D'EQUIPEMENT ---
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
    boolean petsAllow
) {}