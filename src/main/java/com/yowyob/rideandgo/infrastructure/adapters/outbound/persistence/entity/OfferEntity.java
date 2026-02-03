package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import com.yowyob.rideandgo.domain.model.enums.OfferState;
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
import java.util.List;
import java.util.UUID;

@Data 
@NoArgsConstructor 
@AllArgsConstructor
@Table("offers") 
public class OfferEntity implements Persistable<UUID> {
    
    @Id
    private UUID id;

    @Column("passenger_id")
    private UUID passengerId;

    @Column("selected_driver_id") 
    private UUID selectedDriverId;

    @Column("start_point")
    private String startPoint;

   @Column("start_lat") // ✅ Mapping DB
    private Double startLat;

    @Column("start_lon") // ✅ Mapping DB
    private Double startLon;

    @Column("end_point")
    private String endPoint;

    private double price;

    @Column("state")
    private OfferState state;

    // --- NOUVEAUX CHAMPS ---
    @Column("passenger_phone")
    private String passengerPhone;

    @Column("departure_time")
    private String departureTime;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime lastModifiedDate;

    @Transient
    private List<OfferAgreementEntity> agreements;

    @Transient
    private boolean newEntity = false;

    @Override
    @Transient
    public boolean isNew() {
        return this.newEntity || id == null;
    }

    public void setNewEntity(boolean isNew) {
        this.newEntity = isNew;
    }
}