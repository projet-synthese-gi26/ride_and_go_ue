package com.yowyob.rideandgo.domain.model;

import com.yowyob.rideandgo.domain.model.enums.OfferState;
import lombok.Builder;
import java.util.List;
import java.util.UUID;

@Builder
public record Offer(
        UUID id,
        UUID passengerId,
        UUID selectedDriverId, 
        String startPoint,
        Double startLat,    // ✅ Nouvelle coordonnée
        Double startLon,    // ✅ Nouvelle coordonnée
        String endPoint,
        double price,
        String passengerPhone, 
        String departureTime,  
        OfferState state,
        List<Bid> bids, 
        Long version) {

    public Offer withBids(List<Bid> bids) {
        return new Offer(this.id, this.passengerId, this.selectedDriverId, this.startPoint, 
                this.startLat, this.startLon, this.endPoint, this.price, 
                this.passengerPhone, this.departureTime, this.state, bids, this.version);
    }

    public Offer withState(OfferState state) {
        return new Offer(this.id, this.passengerId, this.selectedDriverId, this.startPoint, 
                this.startLat, this.startLon, this.endPoint, this.price, 
                this.passengerPhone, this.departureTime, state, this.bids, this.version);
    }

    public Offer withDriverSelected(UUID driverId) {
        return new Offer(this.id, this.passengerId, driverId, this.startPoint, 
                this.startLat, this.startLon, this.endPoint, this.price, 
                this.passengerPhone, this.departureTime, OfferState.DRIVER_SELECTED, this.bids, this.version);
    }
    
    public boolean hasDriverApplied(UUID driverId) {
        if (bids == null) return false;
        return bids.stream().anyMatch(b -> b.driverId().equals(driverId));
    }
}