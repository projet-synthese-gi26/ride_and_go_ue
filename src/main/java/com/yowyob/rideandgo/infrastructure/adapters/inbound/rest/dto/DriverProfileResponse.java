package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.Vehicle;
import java.util.UUID;

/**
 * Réponse enrichie pour le profil public d'un chauffeur.
 */
public record DriverProfileResponse(
        UUID userId,
        String firstName,          // NOUVEAU
        String lastName,           // NOUVEAU
        String status,
        String licenseNumber,
        boolean isOnline,
        boolean isProfileValidated,
        boolean isSyndicated,
        boolean isProfileCompleted,
        Double rating,             // NOUVEAU
        Integer totalReviewsCount, // NOUVEAU
        Vehicle vehicle) {
}