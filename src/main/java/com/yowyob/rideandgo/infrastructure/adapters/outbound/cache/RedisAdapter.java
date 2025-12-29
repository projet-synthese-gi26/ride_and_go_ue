package com.yowyob.rideandgo.infrastructure.adapters.outbound.cache;

import com.yowyob.rideandgo.domain.model.Fare;
import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.ports.out.FareCachePort;
import com.yowyob.rideandgo.domain.ports.out.OfferCachePort;

import com.yowyob.rideandgo.domain.ports.out.UserCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAdapter implements  OfferCachePort, UserCachePort, FareCachePort {
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

 

    @Override
    public Mono<Boolean> saveInCache(Offer offer) {
        return redisTemplate.opsForValue()
                .set("offer:"+ offer.id(), offer, Duration.ofMinutes(15));
    }

    @Override
    public Mono<Offer> findOfferById(UUID offerId) {
        log.info("findOfferById: {}", offerId);
        return redisTemplate.opsForValue()
                .get("offer:" + offerId)
                .cast(Offer.class)
                .onErrorResume(e -> Mono.empty());
    }

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
}