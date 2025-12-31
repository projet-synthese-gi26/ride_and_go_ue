package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

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
@Table("offer_driver_linkages") 
public class OfferAgreementEntity implements Persistable<UUID> {
    @Id
    private UUID id;

    @Column("driver_id")
    private UUID driverId;

    @Column("offer_id")
    private UUID offerId;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdDate;

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

    public OfferAgreementEntity asNew() {
        this.newEntity = true;
        return this;
    }
}