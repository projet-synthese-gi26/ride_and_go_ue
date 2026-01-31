package com.yowyob.rideandgo.infrastructure.adapters.outbound.cache;

import com.yowyob.rideandgo.domain.model.Fare;
import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.ports.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAdapter
        implements OfferCachePort, UserCachePort, FareCachePort, LocationCachePort, CacheInvalidationPort {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    private static final String LOCATION_KEY_PREFIX = "location:";
    private static final Duration LOCATION_TTL = Duration.ofMinutes(5);

    // --- LocationCachePort Implementation ---

    @Override
    public Mono<Boolean> saveLocation(UUID actorId, Double latitude, Double longitude) {
        String key = LOCATION_KEY_PREFIX + actorId.toString();
        Map<String, Double> coords = Map.of(
                "lat", latitude,
                "lon", longitude);

        return redisTemplate.opsForValue()
                .set(key, coords, LOCATION_TTL)
                .doOnSuccess(success -> log.debug("Location cached for actor {} (success: {})", actorId, success))
                .onErrorResume(e -> {
                    log.error("Failed to cache location for actor {}", actorId, e);
                    return Mono.just(false);
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Location> getLocation(UUID actorId) {
        String key = LOCATION_KEY_PREFIX + actorId.toString();
        return redisTemplate.opsForValue()
                .get(key)
                .map(obj -> {
                    Map<String, Double> coords = (Map<String, Double>) obj;
                    return new Location(coords.get("lat"), coords.get("lon"));
                })
                .doOnError(e -> log.error("Error retrieving location for actor {}", actorId, e))
                .onErrorResume(e -> Mono.empty());
    }

    // --- OfferCachePort Implementation ---

    @Override
    public Mono<Boolean> saveInCache(Offer offer) {
        return redisTemplate.opsForValue()
                .set("offer:" + offer.id(), offer, Duration.ofMinutes(15));
    }

    @Override
    public Mono<Offer> findOfferById(UUID offerId) {
        return redisTemplate.opsForValue()
                .get("offer:" + offerId)
                .cast(Offer.class)
                .onErrorResume(e -> Mono.empty());
    }

    // --- UserCachePort Implementation ---

    @Override
    public Mono<Boolean> saveInCache(User user) {
        return redisTemplate.opsForValue()
                .set("user:" + user.id(), user, Duration.ofMinutes(10));
    }

    @Override
    public Mono<User> findUserById(UUID userId) {
        return redisTemplate.opsForValue()
                .get("user:" + userId)
                .cast(User.class);
    }

    // --- FareCachePort Implementation ---

    @Override
    public Mono<Boolean> saveInCache(Fare fare) {
        return redisTemplate.opsForValue()
                .set("fare:" + fare.id(), fare, Duration.ofMinutes(10));
    }

    @Override
    public Mono<Fare> findFareById(UUID fareId) {
        return redisTemplate.opsForValue()
                .get("fare:" + fareId)
                .cast(Fare.class);
    }

    // --- CacheInvalidationPort Implementation ---

    @Override
    public Mono<Void> invalidateUserCache(UUID userId) {
        String userKey = "user:" + userId.toString();
        String locationKey = LOCATION_KEY_PREFIX + userId.toString();

        log.info("🔥 Invalidating cache for user {}: keys [{}, {}]", userId, userKey, locationKey);

        // Supprime les deux clés de manière réactive
        return Flux.just(userKey, locationKey)
                .flatMap(redisTemplate::delete)
                .then();
    }
}