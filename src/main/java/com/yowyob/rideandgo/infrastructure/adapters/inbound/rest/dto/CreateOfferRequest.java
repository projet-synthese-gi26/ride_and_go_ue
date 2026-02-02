package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.enums.OfferState;

import java.util.UUID;

public record CreateOfferRequest(
        String startPoint,
        String endPoint,
        double price,
        String passengerPhone, // Optionnel, si vide = soi-même
        String departureTime // ISO String ou Format texte
) {
}