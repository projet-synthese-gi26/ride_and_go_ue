package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import com.yowyob.rideandgo.domain.model.enums.RideState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("ride_and_go_ride")
public class RideEntity extends AbstractAuditingEntity{
    @Id
    private UUID id;

    @Column("offer_id")
    private UUID offerId;

    @Column("driver_id")
    private UUID driverId;

    @Column("passenger_id")
    private UUID passengerId;

    double distance;

    int duration;

    RideState state;

    int timeReal;
}
