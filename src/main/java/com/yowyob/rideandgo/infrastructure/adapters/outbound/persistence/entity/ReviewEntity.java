package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("reviews")
public class ReviewEntity implements Persistable<UUID> {
    @Id
    private UUID id;
    @Column("ride_id")
    private UUID rideId;
    @Column("passenger_id")
    private UUID passengerId;
    @Column("driver_id")
    private UUID driverId;
    private int rating;
    private String comment;
    @Column("created_at")
    private LocalDateTime createdAt;

    @Transient
    private boolean newEntity = false;

    @Override
    public boolean isNew() {
        return newEntity || id == null;
    }
}