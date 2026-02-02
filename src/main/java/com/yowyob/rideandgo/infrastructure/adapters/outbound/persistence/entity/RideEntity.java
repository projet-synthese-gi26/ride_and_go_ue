package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import com.yowyob.rideandgo.domain.model.enums.RideState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
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
@Table("rides")
public class RideEntity implements Persistable<UUID> {
    @Id
    private UUID id;

    @Column("offer_id")
    private UUID offerId;

    @Column("driver_id")
    private UUID driverId;

    @Column("passenger_id")
    private UUID passengerId;

    private double distance;

    @Column("time_estimation")  
    private int duration;

    @Column("state")
    private RideState state;

    @Column("real_time") 
    private int timeReal;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdDate = LocalDateTime.now();

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime lastModifiedDate;

    // --- Gestion Insert/Update ---

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