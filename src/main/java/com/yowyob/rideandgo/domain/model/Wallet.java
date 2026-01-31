package com.yowyob.rideandgo.domain.model;

import lombok.Builder;
import java.util.UUID;

@Builder
public record Wallet(
        UUID id,
        UUID ownerId,
        String ownerName,
        double balance) {
}