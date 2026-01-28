package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.UserDeviceEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import java.util.UUID;

public interface UserDeviceR2dbcRepository extends ReactiveCrudRepository<UserDeviceEntity, UUID> {
}