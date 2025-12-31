package com.yowyob.rideandgo.domain.model;

import lombok.Builder;
import java.util.UUID;

/**
 * Domain model representing a driver's application to an offer.
 */
@Builder
public record Bid(
    UUID driverId,
    String driverName,
    Integer eta,              // Temps en minutes
    Double latitude,          
    Double longitude,         
    Double rating,            // Note du chauffeur (ex: 4.8)
    
    // --- Nouveaux champs pour la Démo UI ---
    String carModel,          // ex: "Toyota Yaris"
    String carColor,          // ex: "Rouge"
    String licensePlate,      // ex: "LT-123-AB"
    String driverImage        // URL photo chauffeur
) {}