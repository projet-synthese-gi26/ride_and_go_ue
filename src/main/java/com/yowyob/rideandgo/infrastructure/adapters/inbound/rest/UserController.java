package com.yowyob.rideandgo.infrastructure.adapters.inbound.rest;

import com.yowyob.rideandgo.application.utils.Utils;
import com.yowyob.rideandgo.domain.model.Role;
import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.domain.model.enums.RoleType;
import com.yowyob.rideandgo.domain.ports.in.UserUseCases;
import com.yowyob.rideandgo.domain.ports.out.RoleRepositoryPort;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.CreateUserRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.UserResponse;
import com.yowyob.rideandgo.infrastructure.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Operations related to User profiles")
public class UserController {
    private final UserUseCases userUseCases;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepositoryPort roleRepository;

    /**
     * Creates a new user and assigns a primary role.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user", description = "Creates a new user with a specified role type")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public Mono<UserResponse> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User creation payload", required = true,
                    content = @Content(schema = @Schema(implementation = CreateUserRequest.class)))
            @RequestBody CreateUserRequest request) {
        
        return roleRepository.findByRoleName(request.getType())
                .flatMap(role -> {
                    User userToSave = User.builder()
                            .id(Utils.generateUUID())
                            .name(request.getName())
                            .email(request.getEmail())
                            .telephone(request.getTelephone())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .roles(Set.of(role)) // Wrap role in a Set for the new RBAC domain model
                            .directPermissions(Collections.emptySet()) // Initialize empty direct permissions
                            .build();

                    return userUseCases.saveUser(userToSave);
                })
                .map(savedUser -> {
                    UserResponse response = userMapper.toResponse(savedUser);
                    // Map the first role type back to the DTO for the frontend
                    if (!savedUser.roles().isEmpty()) {
                        response.setRole(savedUser.roles().iterator().next().type());
                    }
                    return response;
                });
    }

    /**
     * Retrieves a detailed user profile including roles and permissions.
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by id", description = "Retrieve a user profile by its UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<UserResponse> getUserById(
            @Parameter(description = "User identifier", required = true) @PathVariable UUID userId) {
        log.info("Fetching user details for id: {}", userId);
        
        return userUseCases.getUserById(userId)
                .map(user -> {
                    UserResponse response = userMapper.toResponse(user);
                    // Set the primary role type in the DTO
                    if (user.roles() != null && !user.roles().isEmpty()) {
                        response.setRole(user.roles().iterator().next().type());
                    }
                    return response;
                });
    }

    /**
     * Lists all users, optionally filtered by their primary role.
     */
    @GetMapping
    @Operation(summary = "List users", description = "Get all users or filter by their primary role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of users", content = @Content(schema = @Schema(implementation = UserResponse.class)))
    })
    public Flux<UserResponse> getAllUsers(
            @Parameter(description = "Optional role filter") @RequestParam(required = false) RoleType role) {
        
        Flux<User> userFlux = (role != null) ? userUseCases.getUsersByRole(role) : userUseCases.getAllUsers();

        return userFlux.map(user -> {
            UserResponse response = userMapper.toResponse(user);
            if (user.roles() != null && !user.roles().isEmpty()) {
                response.setRole(user.roles().iterator().next().type());
            }
            return response;
        });
    }

    /**
     * Deletes a user from the system.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Permanently delete a user by its UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User successfully deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<Boolean> deleteById(
            @Parameter(description = "User identifier", required = true) @PathVariable UUID id) {
        log.info("Deleting user with id: {}", id);
        return userUseCases.deleteUserById(id);
    }
}