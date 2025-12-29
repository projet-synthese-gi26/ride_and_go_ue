package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data 
@NoArgsConstructor
@AllArgsConstructor
@Table("offer_driver_linkages") 
public class OfferAgreementEntity extends AbstractAuditingEntity {
    @Id
    private UUID id;

    @Column("driver_id")
    private UUID driverId;

    @Column("offer_id")
    private UUID offerId;
}