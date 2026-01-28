package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.application.service.AdminService;
import com.yowyob.rideandgo.domain.model.Driver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin-Backoffice", description = "Operations for platform administrators")
@PreAuthorize("hasAuthority('RIDE_AND_GO_ADMIN')") // Protection globale
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/drivers/pending")
    @Operation(summary = "List pending drivers", description = "Get list of drivers waiting for validation")
    public Flux<Driver> getPendingDrivers() {
        return adminService.getPendingDrivers();
    }

    @PatchMapping("/drivers/{id}/validate")
    @Operation(summary = "Validate Driver", description = "Approve driver documents and enable access")
    public Mono<Driver> validateDriver(@PathVariable UUID id) {
        return adminService.validateDriver(id);
    }
}