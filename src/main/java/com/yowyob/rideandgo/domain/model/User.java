package com.yowyob.rideandgo.domain.model;

import lombok.Builder;
import java.util.Set;
import java.util.UUID;

@Builder
public record User(
    UUID id,
    String name,        // Username / Pseudo
    String firstName,   // Prénom
    String lastName,    // Nom
    String email,
    String telephone,
    String password,
    String photoUri,
    Set<Role> roles,
    Set<Permission> directPermissions
) {}