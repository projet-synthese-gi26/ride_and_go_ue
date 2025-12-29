package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.enums.RideState;
import lombok.Data;

import java.util.UUID;

@Data
public class RideResponse {
    private UUID id;

    private UUID offerId;

    private UUID driverId;

    private UUID passengerId;

    double distance;

    int duration;

    RideState state;

    int timeReal;
}