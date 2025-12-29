package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.enums.OfferState;

import java.util.List;
import java.util.UUID;

public record OfferResponse(
        UUID id,
        UUID passengerId,
        String startPoint,
        String endPoint,
        double price,
        OfferState state,
        List<UUID> interestedDrivers
) {}
