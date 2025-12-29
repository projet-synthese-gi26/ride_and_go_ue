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
@Tag(name = "Users", description = "User management endpoints")
public class UserController {
    private final UserUseCases userUseCases;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepositoryPort roleRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user", description = "Creates a new user with a role")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public Mono<UserResponse> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User creation payload", required = true,
                    content = @Content(schema = @Schema(implementation = CreateUserRequest.class)))
            @RequestBody CreateUserRequest request) {
        return Mono.just(request)
                .map(userMapper::toDomain)
                .flatMap(user ->
                        roleRepository.findByRoleName(request.getType())
                                .map(role -> User.builder()
                                        .id(Utils.generateUUID())
                                        .name(user.name())
                                        .email(user.email())
                                        .telephone(user.telephone())
                                        .password(passwordEncoder.encode(user.password()))
                                        .role(role)
                                        .build())
                )
                .flatMap(userUseCases::saveUser)
                .map(user -> User.builder()
                        .id(user.id())
                        .name(user.name())
                        .email(user.email())
                        .telephone(user.telephone())
                        .password(user.password())
                        .role(Role.builder().id(user.role().id()).type(user.role().type()).build())
                        .build())
                .map(userMapper::toResponse);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by id", description = "Retrieve a user by its UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<UserResponse> getUserById(
            @Parameter(description = "User identifier", required = true) @PathVariable UUID userId) {
        log.info("Getting user by id: {}", userId);
        return userUseCases.getUserById(userId)
                .map(user -> {
                    UserResponse response = userMapper.toResponse(user);
                    log.info("User found {}", user);
                    response.setRole(user.role().type());
                    log.info("User found: {}", response);
                    return response;
                });
    }

    @GetMapping
    @Operation(summary = "List users", description = "Get all users or filter by role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of users", content = @Content(schema = @Schema(implementation = UserResponse.class)))
    })
    public Flux<UserResponse> getAllUsers(
            @Parameter(description = "Optional role filter") @RequestParam(required = false) RoleType role) {
        Flux<UserResponse> users;
        if (role != null) {
            users = userUseCases.getUsersByRole(role)
                    .map(userMapper::toResponse)
                    .map(user -> UserResponse.builder()
                            .id(user.getId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .telephone(user.getTelephone())
                            .role(role)
                            .build());
        } else {
            users = userUseCases.getAllUsers()
                    .map(userMapper::toResponse);
        }
        return users;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete a user by its UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<Boolean> deleteById(
            @Parameter(description = "User identifier", required = true) @PathVariable UUID id) {
        return userUseCases.deleteUserById(id);
    }
}