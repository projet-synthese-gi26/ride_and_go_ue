package com.yowyob.rideandgo.domain.model.enums;

/**
 * Lifecycle states of a transport Offer as defined in spec.md
 */
public enum OfferState {
    /** Initial state, waiting for drivers */
    PENDING,
    
    /** At least one driver applied */
    BID_RECEIVED,
    
    /** Passenger chose a driver */
    DRIVER_SELECTED,
    
    /** Driver confirmed, offer is closed and becomes a Ride */
    VALIDATED,
    
    /** Cancelled or expired */
    CANCELLED
}