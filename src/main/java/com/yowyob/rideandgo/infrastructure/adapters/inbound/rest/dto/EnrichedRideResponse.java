package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto;

import com.yowyob.rideandgo.domain.model.Ride;
import com.yowyob.rideandgo.domain.model.Vehicle;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnrichedRideResponse {
    private Ride ride;

    // Infos de l'offre
    private String startPoint;
    private String endPoint;
    private Double price;

    // Infos acteurs
    private String driverName;
    private String driverPhoto;
    private String passengerName;
    private String passengerPhoto;

    // Infos véhicule
    private Vehicle vehicle;
}