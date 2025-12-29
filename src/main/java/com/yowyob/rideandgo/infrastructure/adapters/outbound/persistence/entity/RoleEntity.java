package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import com.yowyob.rideandgo.domain.model.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data 
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor 
@AllArgsConstructor
@Table("roles")
public class RoleEntity extends AbstractAuditingEntity {
    @Id
    private UUID id;

    private RoleType type;
}