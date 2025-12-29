package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.util.UUID;

@Data @EqualsAndHashCode(callSuper = true)
@NoArgsConstructor @AllArgsConstructor
@Table("permissions")
public class PermissionEntity extends AbstractAuditingEntity {
    @Id
    private UUID id;
    private String name;
}