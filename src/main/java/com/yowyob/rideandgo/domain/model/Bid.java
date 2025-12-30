package com.yowyob.rideandgo.domain.model;

import lombok.Builder;
import java.util.UUID;

/**
 * Domain model representing a driver's application to an offer.
 * This is a transient object, computed on-the-fly by aggregating
 * SQL bidder data, Redis location data, and ETA calculation.
 */
@Builder
public record Bid(
    UUID driverId,
    String driverName,
    Integer eta,              // Estimated Time of Arrival in minutes (computed)
    Double latitude,          // Last known latitude from Redis
    Double longitude,         // Last known longitude from Redis
    Double rating             // Average driver rating
) {}