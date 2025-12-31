package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.enums.RideState;

public record UpdateStatusRequest(
    RideState status
) {}