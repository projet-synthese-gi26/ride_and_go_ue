package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.Bid;
import com.yowyob.rideandgo.domain.model.enums.OfferState;
import com.yowyob.rideandgo.domain.model.Bid;

import java.util.List;
import java.util.UUID;

public record OfferResponse(
        UUID id,
        UUID passengerId,
        UUID selectedDriverId,
        String startPoint,
        Double startLat, // ✅ AJOUTÉ
        Double startLon, // ✅ AJOUTÉ
        String endPoint,
        String endLat, // ✅ Pour dessiner la ligne d'arrivée sur la map
        String endLon, // ✅
        double price,
        String passengerPhone,
        String departureTime,
        OfferState state,
        List<Bid> bids 
) {}
