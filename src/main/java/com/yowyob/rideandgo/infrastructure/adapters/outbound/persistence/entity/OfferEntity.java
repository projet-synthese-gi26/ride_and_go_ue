package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import com.yowyob.rideandgo.domain.model.enums.OfferState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data 
@NoArgsConstructor 
@AllArgsConstructor
@Table("offers") 
public class OfferEntity extends AbstractAuditingEntity implements Persistable<UUID> {
    
    @Id
    private UUID id;

    @Column("passenger_id")
    private UUID passengerId;

    @Column("start_point")
    private String startPoint;

    @Column("end_point")
    private String endPoint;

    private double price;

    private OfferState state;

    @Transient
    private List<OfferAgreementEntity> agreements;

    @Transient
    private boolean newEntity = false; // Flag interne

    @Override
    @Transient
    public boolean isNew() {
        return this.newEntity || id == null;
    }

    // Le setter que l'Adapter utilise
    public void setNewEntity(boolean isNew) {
        this.newEntity = isNew;
    }

    @Transient
    @Override
    public Long getVersion() {
        return super.getVersion();
    }
}