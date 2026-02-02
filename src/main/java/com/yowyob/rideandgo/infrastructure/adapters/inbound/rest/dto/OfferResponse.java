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
        String endPoint,
        double price,
        String passengerPhone,
        String departureTime,
        OfferState state,
        List<Bid> bids 
) {}
