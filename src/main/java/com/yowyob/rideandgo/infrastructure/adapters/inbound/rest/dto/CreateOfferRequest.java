package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.enums.OfferState;

import java.util.UUID;

public record CreateOfferRequest(
        String startPoint,
        Double startLat,
        Double startLon,
        String endPoint,
        Double endLat, // ✅ AJOUTÉ
        Double endLon, // ✅ AJOUTÉ
        double price,
        String passengerPhone, 
        String departureTime 
) {}