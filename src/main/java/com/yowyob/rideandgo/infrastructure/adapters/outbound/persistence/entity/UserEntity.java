package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class UserEntity implements Persistable<UUID> {
    @Id
    private UUID id;

    private String name; // Utilisé pour le username

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("email_address")
    private String email;

    @Column("phone_number")
    private String telephone;

    @Column("photo_uri")
    private String photoUri;

    @Transient
    private boolean newEntity = false;

    @Override
    @Transient
    public boolean isNew() {
        return this.newEntity || id == null;
    }

    public void setNewEntity(boolean newEntity) {
        this.newEntity = newEntity;
    }
}