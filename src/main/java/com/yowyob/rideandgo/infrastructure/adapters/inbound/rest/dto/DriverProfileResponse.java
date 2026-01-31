package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.Vehicle;
import java.util.UUID;

/**
 * Réponse complète incluant le profil chauffeur et le véhicule enrichi (images
 * incluses).
 */
public record DriverProfileResponse(
        UUID userId,
        String status,
        String licenseNumber,
        boolean isOnline,
        boolean isProfileValidated,
        boolean isSyndicated,
        boolean isProfileCompleted,
        Vehicle vehicle) {
}