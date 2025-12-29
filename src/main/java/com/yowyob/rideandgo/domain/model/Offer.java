package com.yowyob.rideandgo.domain.model;

import com.yowyob.rideandgo.domain.model.enums.OfferState;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record Offer(
        UUID id,
        UUID passengerId,
        String startPoint,
        String endPoint,
        double price,
        OfferState state,
        List<UUID> interestedDrivers,
        Long version) {

    public Offer withInterestedDriversAndState(List<UUID> interestedDrivers, OfferState state) {
        return new Offer(this.id, this.passengerId, this.startPoint, this.endPoint, this.price, state, interestedDrivers, this.version);
    }

    public Offer withInterestedDrivers(List<UUID> interestedDrivers) {
        return new Offer(this.id, this.passengerId, this.startPoint, this.endPoint, this.price, this.state, interestedDrivers, this.version);
    }

    public Offer withState(OfferState state) {
        return new Offer(this.id, this.passengerId, this.startPoint, this.endPoint, this.price, state, this.interestedDrivers, this.version);
    }
}