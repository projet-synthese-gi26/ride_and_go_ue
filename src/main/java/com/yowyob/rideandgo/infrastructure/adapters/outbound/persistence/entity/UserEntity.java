package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data @EqualsAndHashCode(callSuper = true)
@NoArgsConstructor @AllArgsConstructor
@Table("ride_and_go_user")
public class UserEntity extends AbstractAuditingEntity{
    @Id
    private UUID id;

    private String name;
    
    private String email;

    private String telephone;

    private String password;

    @Column("role_id")
    private UUID roleId;
}