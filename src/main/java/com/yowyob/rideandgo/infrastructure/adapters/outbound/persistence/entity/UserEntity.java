package com.yowyob.rideandgo.infrastructure.adapters.outbound.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable; // <--- Import Important
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data 
@NoArgsConstructor 
@AllArgsConstructor
@Table("users") 
public class UserEntity implements Persistable<UUID> { // <--- Implémente Persistable
    @Id
    private UUID id;

    private String name;
    
    @Column("email_address") 
    private String email;

    @Column("phone_number") 
    private String telephone;

    @Column("photo_uri")
    private String photoUri;

    @Transient 
    private String password; 

    // --- Gestion Insert/Update (Magie R2DBC) ---

    @Transient
    private boolean newEntity = false;

    @Override
    @Transient
    public boolean isNew() {
        // Si on marque explicitement 'newEntity' à true, ou si l'ID est null (cas classique)
        return this.newEntity || id == null;
    }

    public void setNewEntity(boolean newEntity) {
        this.newEntity = newEntity;
    }
}