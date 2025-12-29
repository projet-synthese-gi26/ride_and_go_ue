package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data 
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor 
@AllArgsConstructor
@Table("users") 
public class UserEntity extends AbstractAuditingEntity {
    @Id
    private UUID id;

    private String name;
    
    @Column("email_address") 
    private String email;

    @Column("phone_number") 
    private String telephone;

    private String password;


}