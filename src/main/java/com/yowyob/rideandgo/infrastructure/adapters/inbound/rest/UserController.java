package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.rideandgo.application.service.UserService;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.Vehicle;
import com.yowyob.rideandgo.domain.ports.in.UserUseCases;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.BecomeDriverRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.ChangePasswordRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.DriverProfileResponse;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.FullDriverProfileResponse;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.UpdateUserProfileRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.UserResponse;
import com.yowyob.rideandgo.infrastructure.mappers.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management")
public class UserController {
    private final UserUseCases userUseCases;
    private final UserMapper userMapper;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    // --- GESTION ADMIN ---

    @GetMapping("/service/{serviceName}")
    @Operation(summary = "List all users of a service (Admin)", description = "Fetches users from remote Auth Service.")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_ADMIN')")
    public Flux<UserResponse> getAllUsersByService(@PathVariable String serviceName) {
        return userUseCases.getAllRemoteUsersByService(serviceName)
                .map(this::mapToResponse);
    }

    // --- GESTION PROFIL (SELF) ---

    @GetMapping("/me")
    @Operation(summary = "Get my profile", description = "Retrieve the profile of the currently authenticated user based on JWT.")
    public Mono<UserResponse> getCurrentUser() {
        return getCurrentUserId()
                .flatMap(userUseCases::getUserById)
                .map(this::mapToResponse);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update my profile")
    public Mono<UserResponse> updateProfile(@RequestBody UpdateUserProfileRequest request) {
        return getCurrentUserId()
                .flatMap(userId -> userUseCases.updateProfile(userId, request.firstName(), request.lastName(),
                        request.phone()))
                .map(this::mapToResponse);
    }

    @PutMapping("/password")
    @Operation(summary = "Change my password")
    public Mono<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        return getCurrentUserId()
                .flatMap(userId -> userUseCases.changePassword(userId, request.currentPassword(),
                        request.newPassword()));
    }

    @PostMapping(value = "/driver", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Become Driver - Full Onboarding")
    public Mono<DriverProfileResponse> becomeDriver(
            @RequestPart("data") BecomeDriverRequest request, // ✅ Objet structuré
            @RequestPart(name = "registrationPhoto", required = false) FilePart registrationPhoto,
            @RequestPart(name = "serialPhoto", required = false) FilePart serialPhoto) {

        return getCurrentUserId()
                .flatMap(userId -> userService.upgradeToDriverComplete(
                        userId, request, registrationPhoto, serialPhoto));
    }

    /**
     * NOUVEAU : Route pour déclencher la vérification du statut Syndicat.
     */
    @PatchMapping("/verify-compliance")
    @Operation(summary = "Verify Syndicate Membership", description = "Checks with UGate if the driver is verified. If yes, completes the profile.")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_DRIVER')")
    public Mono<DriverProfileResponse> verifyCompliance() {
        return getCurrentUserId()
                .flatMap(userUseCases::verifySyndicateStatus);
    }

    @GetMapping("/me/driver-profile")
    @Operation(summary = "Get Full Driver Profile", description = "Aggregates User, Driver, Wallet and Vehicle data.")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_DRIVER')")
    public Mono<FullDriverProfileResponse> getFullDriverProfile() {
        return getCurrentUserId()
                .flatMap(userService::getFullDriverProfile);
    }

    // --- LECTURE STANDARD ---

    @GetMapping("/{userId}")
    public Mono<UserResponse> getUserById(@PathVariable UUID userId) {
        return userUseCases.getUserById(userId).map(this::mapToResponse);
    }

    @GetMapping("/drivers/{id}")
    @Operation(summary = "Get Driver Public Profile", description = "Get details about a specific driver including their vehicle.")
    public Mono<DriverProfileResponse> getDriverById(@PathVariable UUID id) {
        return userService.getDriverProfile(id);
    }

    // --- HELPERS ---

    private Mono<UUID> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> UUID.fromString(auth.getName()));
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = userMapper.toResponse(user);
        if (user.roles() != null) {
            response.setRoles(user.roles().stream()
                    .map(com.yowyob.rideandgo.domain.model.Role::type)
                    .collect(Collectors.toList()));
        }
        return response;
    }
}