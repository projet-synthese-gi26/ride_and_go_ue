package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.NotificationSettingsEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import java.util.UUID;

public interface NotificationSettingsR2dbcRepository extends ReactiveCrudRepository<NotificationSettingsEntity, UUID> {
}