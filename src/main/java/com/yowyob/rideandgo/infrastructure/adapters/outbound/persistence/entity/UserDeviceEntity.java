package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("user_devices")
public class UserDeviceEntity implements Persistable<UUID> {

    @Id
    @Column("user_id")
    private UUID userId;

    @Column("device_token")
    private String deviceToken;

    private String platform;

    @LastModifiedDate
    @Column("last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Transient
    private boolean newEntity = false;

    @Override
    @Transient
    public boolean isNew() {
        return newEntity || userId == null;
    }
    
    // UUID est manuel (FK), donc on doit gérer isNew manuellement
    @Override
    public UUID getId() {
        return userId;
    }
}