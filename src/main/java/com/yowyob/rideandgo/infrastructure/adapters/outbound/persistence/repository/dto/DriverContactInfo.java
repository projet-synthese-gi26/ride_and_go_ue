package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.dto;

import java.util.UUID;

// Projection simple pour récupérer les infos de contact
public record DriverContactInfo(
    UUID driverId,
    String email,
    String phone
) {}