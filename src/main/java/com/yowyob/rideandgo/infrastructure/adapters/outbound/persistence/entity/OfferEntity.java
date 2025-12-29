package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import com.yowyob.rideandgo.domain.model.enums.OfferState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data 
@NoArgsConstructor 
@AllArgsConstructor
@Table("offers") 
public class OfferEntity extends AbstractAuditingEntity {
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
}