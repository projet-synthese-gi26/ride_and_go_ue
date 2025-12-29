package com.yowyob.rideandgo.domain.model;

import com.yowyob.rideandgo.domain.model.enums.RoleType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record Role(UUID id, RoleType type) {
}
