package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.Driver;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.Vehicle;
import com.yowyob.rideandgo.domain.model.Wallet;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FullDriverProfileResponse {
    private UserResponse user;
    private Driver driver;
    private Wallet wallet;
    private Vehicle vehicle;
}