package com.yowyob.rideandgo.domain.model;

import com.yowyob.rideandgo.domain.model.enums.RoleType;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Role(
    UUID id, 
    RoleType type,
    Set<Permission> permissions
) {}