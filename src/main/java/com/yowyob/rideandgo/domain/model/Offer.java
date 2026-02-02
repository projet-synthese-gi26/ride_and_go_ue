package com.yowyob.rideandgo.domain.model;

import com.yowyob.rideandgo.domain.model.enums.OfferState;
import lombok.Builder;
import java.util.List;
import java.util.UUID;

/**
 * Modèle de Domaine représentant une offre de transport.
 * Inclut désormais le support pour la commande pour tiers (passengerPhone) 
 * et l'heure de départ prévue (departureTime).
 */
@Builder
public record Offer(
        UUID id,
        UUID passengerId,
        UUID selectedDriverId, 
        String startPoint,
        String endPoint,
        double price,
        String passengerPhone, // Numéro du passager réel (soi-même ou tiers)
        String departureTime,  // Heure de départ fournie par le front
        OfferState state,
        List<Bid> bids, 
        Long version) {

    /**
     * Retourne une nouvelle instance de l'offre avec une liste de candidatures mise à jour.
     */
    public Offer withBids(List<Bid> bids) {
        return new Offer(this.id, this.passengerId, this.selectedDriverId, this.startPoint, this.endPoint, this.price, 
                this.passengerPhone, this.departureTime, this.state, bids, this.version);
    }

    /**
     * Retourne une nouvelle instance de l'offre avec un nouvel état.
     */
    public Offer withState(OfferState state) {
        return new Offer(this.id, this.passengerId, this.selectedDriverId, this.startPoint, this.endPoint, this.price, 
                this.passengerPhone, this.departureTime, state, this.bids, this.version);
    }

    /**
     * Helper pour mettre à jour l'état ET assigner le chauffeur choisi.
     */
    public Offer withDriverSelected(UUID driverId) {
        return new Offer(this.id, this.passengerId, driverId, this.startPoint, this.endPoint, this.price, 
                this.passengerPhone, this.departureTime, OfferState.DRIVER_SELECTED, this.bids, this.version);
    }
    
    /**
     * Vérifie si un chauffeur spécifique a déjà postulé à cette offre.
     */
    public boolean hasDriverApplied(UUID driverId) {
        if (bids == null) return false;
        return bids.stream().anyMatch(b -> b.driverId().equals(driverId));
    }
}