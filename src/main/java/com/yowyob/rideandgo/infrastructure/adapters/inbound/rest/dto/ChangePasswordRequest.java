package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

public record ChangePasswordRequest(
    String currentPassword,
    String newPassword
) {}