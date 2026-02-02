package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.enums.RideState;
import com.yowyob.rideandgo.domain.model.Vehicle;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@Builder
public class EnrichedRideResponse {
    private UUID rideId;
    private RideState state;
    private Double distance;
    private Double price;
    private String startPoint;
    private String endPoint;
    private LocalDateTime createdAt; // ✅ S'assurer que ce champ est bien là

    // Infos sur le partenaire
    private String partnerName;
    private String partnerPhone;
    private String partnerPhoto;

    // Infos véhicule
    private Vehicle vehicle;
}