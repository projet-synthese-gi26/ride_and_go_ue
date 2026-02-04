package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import java.time.LocalDateTime;

public record LandingOfferResponse(
    String startPoint,
    String endPoint,
    Double startLat,
    Double startLon,
    Double endLat, // ✅ Pour dessiner la ligne d'arrivée sur la map
    Double endLon, // ✅
    double price,
    String departureTime,
    LocalDateTime createdAt
) {}