package com.yowyob.rideandgo.infrastructure.mappers;

import com.yowyob.rideandgo.domain.model.User;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.CreateUserRequest;
import com.yowyob.rideandgo.infrastructure.adapters.inbound.rest.dto.UserResponse;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity.UserEntity;
import com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.repository.RoleR2dbcRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import reactor.core.publisher.Mono;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "role", ignore = true)
    User toDomain(UserEntity entity);

    default Mono<User> toDomain(UserEntity entity, RoleR2dbcRepository roleRepository, RoleMapper roleMapper) {
        User user = toDomain(entity);
        return roleRepository.findById(entity.getRoleId())
                .map(roleMapper::toDomain)
                .map(role -> User.builder()
                        .id(user.id())
                        .name(user.name())
                        .email(user.email())
                        .telephone(user.telephone())
                        .role(role)
                        .build()
                );
    }

    @Mapping(target = "roleId", source = "role.id")
    UserEntity toEntity(User domain);

    User toDomain(CreateUserRequest request);

    @Mapping(target = "role", source = "role.type")
    UserResponse toResponse(User domain);
}
