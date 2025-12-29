package com.yowyob.rideandgo.domain.model;

import lombok.Builder;
import java.util.Set;
import java.util.UUID;

@Builder
public record User(
    UUID id,
    String name,
    String email,
    String telephone,
    String password,
    Set<Role> roles,
    Set<Permission> directPermissions
) {}