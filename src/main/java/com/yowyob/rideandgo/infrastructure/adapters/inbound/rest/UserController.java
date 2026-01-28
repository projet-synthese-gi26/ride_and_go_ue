package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.application.service.UserService;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.ports.in.UserUseCases;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.BecomeDriverRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.ChangePasswordRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.UpdateUserProfileRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.UserResponse;
import com.yowyob.rideandgo.infrastructure.mappers.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    // --- GESTION ADMIN ---

    @GetMapping("/service/{serviceName}")
    @Operation(summary = "List all users of a service (Admin)", description = "Fetches users from remote Auth Service.")
    @PreAuthorize("hasAuthority('RIDE_AND_GO_ADMIN')")
    public Flux<UserResponse> getAllUsersByService(@PathVariable String serviceName) {
        return userUseCases.getAllRemoteUsersByService(serviceName)
                .map(this::mapToResponse);
    }

    @GetMapping("/me")
    @Operation(summary = "Get my profile", description = "Retrieve the profile of the currently authenticated user based on JWT.")
    public Mono<UserResponse> getCurrentUser() {
        return getCurrentUserId()
                .flatMap(userUseCases::getUserById)
                .map(this::mapToResponse);
    }

    // --- GESTION PROFIL (SELF) ---

    @PutMapping("/profile") // Préférable de ne pas passer l'ID dans l'URL pour la sécurité (utilise le token)
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

    @PostMapping("/driver")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Become a Driver (Onboarding)", description = "Creates Driver profile, registers Vehicle, and assigns Role.")
    public Mono<Void> becomeDriver(@RequestBody BecomeDriverRequest request) {
        return getCurrentUserId()
                .flatMap(userId -> userService.upgradeToDriverComplete(userId, request));
    }   
    
    // --- LECTURE STANDARD ---

    @GetMapping("/{userId}")
    public Mono<UserResponse> getUserById(@PathVariable UUID userId) {
        return userUseCases.getUserById(userId).map(this::mapToResponse);
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