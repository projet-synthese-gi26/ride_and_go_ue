package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.DriverEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;

public interface DriverR2dbcRepository extends ReactiveCrudRepository<DriverEntity, UUID> {
    @Query("""
                SELECT ud.device_token
                FROM drivers d
                JOIN user_devices ud ON d.id = ud.user_id
                WHERE d.is_online = true
                  AND d.is_profile_completed = true
                  AND d.is_profile_validated = true
            """)
    Flux<String> findDeviceTokensOfActiveDrivers();

    @Query("""
                SELECT u.email_address
                FROM drivers d
                JOIN users u ON d.id = u.id
                WHERE d.is_online = true
                  AND d.is_profile_completed = true
                  AND d.is_profile_validated = true
            """)
    Flux<String> findEmailsOfEligibleDrivers();

    Flux<DriverEntity> findByIsProfileValidatedFalse();
}
