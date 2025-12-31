package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

public record RideTrackingResponse(
    Double latitude,       // La position de la cible
    Double longitude,
    Double distanceKm,     // Distance calculée
    Integer etaMinutes,    // Temps estimé
    String targetRole      // "DRIVER" ou "PASSENGER"
) {}