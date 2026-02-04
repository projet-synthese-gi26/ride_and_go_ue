package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import java.time.LocalDateTime;

public record LandingOfferResponse(
    String startPoint,
    String endPoint,
    Double startLat,    // ✅ Pour afficher un point sur la carte
    Double startLon,    // ✅ Pour afficher un point sur la carte
    double price,
    String departureTime,
    LocalDateTime createdAt // ✅ Le front calculera "Il y a X min" lui-même
) {}