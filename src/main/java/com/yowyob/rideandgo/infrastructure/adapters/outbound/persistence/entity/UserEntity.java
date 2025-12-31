package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data 
@NoArgsConstructor 
@AllArgsConstructor
@Table("users") 
public class UserEntity { // Plus d'héritage
    @Id
    private UUID id;

    private String name;
    
    @Column("email_address") 
    private String email;

    @Column("phone_number") 
    private String telephone;

    // Note: Le mot de passe n'est pas dans le schéma SQL 'users' fourni
    // Je le garde ici pour que ça compile, mais attention au runtime si la colonne manque.
    private String password; 
}