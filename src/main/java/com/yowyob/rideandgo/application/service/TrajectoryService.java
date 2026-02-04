package com.yowyob.rideandgo.application.service;

import com.yowyob.rideandgo.domain.model.DriverTrajectory;
import com.yowyob.rideandgo.domain.ports.out.DriverTrajectoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrajectoryService {
    private final DriverTrajectoryRepositoryPort repositoryPort;

    public Flux<DriverTrajectory> getMyTrajectories(UUID driverId) {
        return repositoryPort.findAllByDriverId(driverId);
    }
}