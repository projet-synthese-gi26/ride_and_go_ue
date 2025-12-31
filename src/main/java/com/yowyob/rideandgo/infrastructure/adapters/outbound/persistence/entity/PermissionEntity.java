package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor 
@AllArgsConstructor
@Table("permissions")
public class PermissionEntity {
    @Id
    private UUID id;
    private String name;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime lastModifiedDate;
}