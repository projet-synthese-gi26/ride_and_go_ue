package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.enums.RoleType;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class CreateUserRequest {
    private String email;

    private String name;

    private String telephone;

    private String password;

    private RoleType type;
}