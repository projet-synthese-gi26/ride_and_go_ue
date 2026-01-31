package com.yowyob.rideandgo.domain.ports.out;

import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Port for cache invalidation operations.
 */
public interface CacheInvalidationPort {
    /**
     * Invalidates all cache entries directly related to a user.
     * 
     * @param userId The ID of the user whose cache should be cleared.
     * @return A Mono that completes when the operation is done.
     */
    Mono<Void> invalidateUserCache(UUID userId);
}