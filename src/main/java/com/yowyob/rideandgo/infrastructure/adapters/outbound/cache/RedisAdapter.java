package com.yowyob.rideandgo.infrastructure.adapters.outbound.cache;

import com.yowyob.rideandgo.domain.model.Fare;
import com.yowyob.rideandgo.domain.model.Offer;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.ports.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAdapter
        implements OfferCachePort, UserCachePort, FareCachePort, LocationCachePort, CacheInvalidationPort {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    // CLÉ 1 : Le "Live" (Geo Set unique pour tous les drivers)
    // Contient : {Membre: "uuid", Score: GeoHash}
    private static final String KEY_GEO_LIVE = "drivers:geo:live";

    // CLÉ 2 : Le "Buffer" (Préfixe pour les listes d'historique)
    // Contient : Liste de strings "lat,lon,timestamp"
    private static final String PREFIX_HISTORY = "history:driver:";

    private static final String KEY_OFFERS_GEO = "offers:geo:pending";

    // --- LocationCachePort Implementation ---

    @Override
    public Mono<Boolean> saveLocation(UUID actorId, Double latitude, Double longitude) {
        String driverIdStr = actorId.toString();
        // Redis utilise (Longitude, Latitude) pour ses Points
        Point point = new Point(longitude, latitude);

        // 1. Mise à jour du Live (GEOADD)
        // Ajoute ou met à jour la position du membre dans le Set géospatial
        Mono<Long> geoAdd = redisTemplate.opsForGeo()
                .add(KEY_GEO_LIVE, point, driverIdStr);

        // 2. Ajout à l'historique (RPUSH)
        // Format compact pour économiser la RAM : "lat,lon,timestamp"
        String historyEntry = String.format("%f,%f,%d", latitude, longitude, Instant.now().getEpochSecond());
        String historyKey = PREFIX_HISTORY + driverIdStr;

        Mono<Long> listPush = redisTemplate.opsForList()
                .rightPush(historyKey, historyEntry);

        // 3. Sécurité : TTL sur la liste d'historique (1h).
        // Si le Cron plante, ces données seront perdues après 1h mais la RAM sera
        // libérée.
        Mono<Boolean> setTtl = redisTemplate.expire(historyKey, Duration.ofHours(1));

        // Exécution parallèle (Pipelining)
        return Mono.zip(geoAdd, listPush, setTtl)
                .map(tuple -> true)
                .doOnError(e -> log.error("❌ Failed to update location for {}", actorId, e))
                .onErrorReturn(false);
    }

    @Override
    public Mono<Location> getLocation(UUID actorId) {
        // opsForGeo().position() retourne un Mono<Point> correspondant au membre
        // demandé.
        // Si le membre n'est pas dans le Set, le Mono est vide.
        return redisTemplate.opsForGeo()
                .position(KEY_GEO_LIVE, actorId.toString())
                .map(p -> new Location(p.getY(), p.getX())); // Point.y = Lat, Point.x = Lon
    }

    @Override
    public Flux<GeoResult> findNearbyDrivers(Double latitude, Double longitude, Double radiusKm) {
        // 1. Définir le point central (Le passager)
        Point center = new Point(longitude, latitude); // Rappel: Redis c'est (Lon, Lat)

        // 2. Définir le rayon de recherche
        // Distance prend (valeur, métrique). KILOMETERS est une constante de Spring
        // Data Redis.
        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);

        // 3. Définir le cercle de recherche
        org.springframework.data.geo.Circle circle = new org.springframework.data.geo.Circle(center, radius);

        // 4. Configurer la commande Redis (GEOSEARCH)
        // - includeDistance() : On veut savoir à quelle distance ils sont
        // - includeCoordinates() : On veut leur position exacte actuelle
        // - sortAscending() : On veut les plus proches en premier
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending();

        // 5. Exécuter la commande
        return redisTemplate.opsForGeo()
                .radius(KEY_GEO_LIVE, circle, args)
                .flatMap(geoResult -> {
                    // Mapping du résultat Redis (GeoResult<RedisGeoCommands.GeoLocation<Object>>)
                    // vers notre objet de domaine (LocationCachePort.GeoResult)

                    String driverIdStr = (String) geoResult.getContent().getName();
                    Point driverPoint = geoResult.getContent().getPoint();
                    double dist = geoResult.getDistance().getValue(); // Distance dans l'unité demandée (km)

                    try {
                        UUID driverId = UUID.fromString(driverIdStr);

                        // Création de notre objet Location interne
                        Location loc = new Location(driverPoint.getY(), driverPoint.getX()); // Y=Lat, X=Lon

                        // Création du résultat final
                        return Mono.just(new GeoResult(driverId, dist, loc));

                    } catch (IllegalArgumentException e) {
                        log.warn("⚠️ Found invalid UUID in Geo Set: {}", driverIdStr);
                        return Mono.empty();
                    }
                });
    }

    // --- CacheInvalidationPort Implementation ---

    @Override
    public Mono<Void> invalidateUserCache(UUID userId) {
        String userIdStr = userId.toString();
        String userKey = "user:" + userIdStr;

        log.info("🔥 Invalidating cache for user {}", userId);

        // 1. Supprimer le cache profil utilisateur
        Mono<Boolean> delUser = redisTemplate.delete(userKey).map(l -> l > 0);

        // 2. Supprimer de la carte Live (ZREM / GEO REMOVE)
        // Cela le rend invisible pour la recherche de taxi
        Mono<Long> delGeo = redisTemplate.opsForGeo().remove(KEY_GEO_LIVE, userIdStr);

        // Note: On NE supprime PAS l'historique (historyKey) ici !
        // On veut que le Cron puisse le traiter et le dumper en base même si l'user se
        // déconnecte.
        // L'historique expirera tout seul grâce au TTL si le cron ne passe pas.

        return Mono.when(delUser, delGeo);
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

    @Override
    public Mono<Void> saveOfferLocation(UUID offerId, Double lat, Double lon) {
        return redisTemplate.opsForGeo()
                .add(KEY_OFFERS_GEO, new Point(lon, lat), offerId.toString())
                .then();
    }

    @Override
    public Mono<Void> removeOfferLocation(UUID offerId) {
        return redisTemplate.opsForGeo()
                .remove(KEY_OFFERS_GEO, offerId.toString())
                .then();
    }

    @Override
    public Flux<UUID> findNearbyOfferIds(Double lat, Double lon, Double radiusKm) {
        Circle circle = new Circle(new Point(lon, lat), new Distance(radiusKm, Metrics.KILOMETERS));
        return redisTemplate.opsForGeo()
                .radius(KEY_OFFERS_GEO, circle)
                .map(res -> UUID.fromString(res.getContent().getName().toString()));
    }
}