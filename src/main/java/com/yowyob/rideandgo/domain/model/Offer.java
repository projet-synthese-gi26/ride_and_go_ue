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
        String endPoint,
        double price,
        OfferState state,
        List<Bid> bids, 
        Long version) {

    public Offer withBids(List<Bid> bids) {
        return new Offer(this.id, this.passengerId, this.selectedDriverId, this.startPoint, this.endPoint, this.price, this.state, bids, this.version);
    }

    public Offer withState(OfferState state) {
        return new Offer(this.id, this.passengerId, this.selectedDriverId, this.startPoint, this.endPoint, this.price, state, this.bids, this.version);
    }

    // Helper pour mettre à jour l'état ET le chauffeur
    public Offer withDriverSelected(UUID driverId) {
        return new Offer(this.id, this.passengerId, driverId, this.startPoint, this.endPoint, this.price, OfferState.DRIVER_SELECTED, this.bids, this.version);
    }
    
    public boolean hasDriverApplied(UUID driverId) {
        if (bids == null) return false;
        return bids.stream().anyMatch(b -> b.driverId().equals(driverId));
    }
}