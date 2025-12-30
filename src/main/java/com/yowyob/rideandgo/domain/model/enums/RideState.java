package com.yowyob.rideandgo.domain.model.enums;

/**
 * Lifecycle states of a Trip (Ride) as defined in spec.md
 */
public enum RideState {
    /** Ride initialized, driver approaching passenger */
    CREATED,
    
    /** Passenger picked up, trip in progress */
    ONGOING,
    
    /** Trip finished at destination */
    COMPLETED,
    
    /** Trip interrupted or cancelled */
    CANCELLED
}