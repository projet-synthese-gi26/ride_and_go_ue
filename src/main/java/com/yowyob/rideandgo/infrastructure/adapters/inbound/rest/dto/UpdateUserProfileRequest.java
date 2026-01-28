package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

public record UpdateUserProfileRequest(
    String firstName,
    String lastName,
    String phone
) {}