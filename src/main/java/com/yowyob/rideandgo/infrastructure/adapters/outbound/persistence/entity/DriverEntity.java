package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("drivers")
public class DriverEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    private String status;
    @Column("license_number")
    private String licenseNumber;
    @Column("has_car")
    private boolean hasCar;
    @Column("is_online")
    private boolean isOnline;
    @Column("is_profile_completed")
    private boolean isProfileCompleted;

    // --- NOUVEAUX CHAMPS ---
    @Column("vehicle_id")
    private UUID vehicleId;

    @Column("is_profile_validated")
    private boolean isProfileValidated;

    @Column("is_syndicated")
    private boolean isSyndicated;

    @Transient
    private boolean newEntity = false;

    @Override
    @Transient
    public boolean isNew() {
        return this.newEntity || id == null;
    }

    public void setNewEntity(boolean newEntity) {
        this.newEntity = newEntity;
    }
}