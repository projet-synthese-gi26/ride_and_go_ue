--- FILE: src/main/resources/db/changelog/changes/000-initial-schema.sql ---
-- liquibase formatted sql

-- changeset yowyob:000-init-schema
-- comment: Initialisation du schéma Ride & Go

-- 1. Création du schéma
CREATE SCHEMA IF NOT EXISTS ride_and_go;

-- 2. Extension UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 3. Types ENUM & Casts (Crucial pour R2DBC)
DO $$ BEGIN
    CREATE TYPE offer_state_enum AS ENUM ('PENDING', 'BID_RECEIVED', 'DRIVER_SELECTED', 'VALIDATED', 'CANCELLED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE ride_state_enum AS ENUM ('CREATED', 'ONGOING', 'COMPLETED', 'CANCELLED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Création des CASTs implicites pour que Java String -> Postgres Enum fonctionne
DO $$ BEGIN
    CREATE CAST (character varying as offer_state_enum) WITH INOUT AS IMPLICIT;
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE CAST (character varying as ride_state_enum) WITH INOUT AS IMPLICIT;
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 4. Table USERS (Socle commun)
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY, -- Pas de default car vient souvent de l'Auth Service
    name VARCHAR(255),
    email_address VARCHAR(255) UNIQUE,
    phone_number VARCHAR(50)
);

-- 5. Table DRIVERS (Extension de Users)
-- Un user devient driver en ayant une entrée ici
CREATE TABLE IF NOT EXISTS drivers (
    id UUID PRIMARY KEY, -- Même ID que users
    status VARCHAR(50) DEFAULT 'OFFLINE', -- AVAILABLE, BUSY, OFFLINE
    license_number VARCHAR(100),
    has_car BOOLEAN DEFAULT FALSE,
    is_online BOOLEAN DEFAULT FALSE,
    is_profile_completed BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_drivers_users FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Table CUSTOMERS (Extension pour les passagers)
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY, -- Même ID que users
    code VARCHAR(50),
    payment_method VARCHAR(50), -- CASH, CARD...
    amount_paid DECIMAL(10, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_customers_users FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

-- 6. Table BUSINESS_ACTORS (Socle pour les pros)
CREATE TABLE IF NOT EXISTS business_actors (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    phone_number VARCHAR(50),
    email_address VARCHAR(255),
    
    CONSTRAINT fk_business_actors_users FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);


-- 6. Tables NOTIFICATIONS (Settings, Devices, History)
CREATE TABLE IF NOT EXISTS user_devices (
    user_id UUID PRIMARY KEY,
    device_token TEXT NOT NULL,
    platform VARCHAR(20),
    last_updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_devices_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notification_settings (
    user_id UUID PRIMARY KEY,
    enable_email BOOLEAN DEFAULT TRUE,
    enable_sms BOOLEAN DEFAULT FALSE,
    enable_push BOOLEAN DEFAULT TRUE,
    enable_whatsapp BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_settings_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    title VARCHAR(255),
    message TEXT,
    type VARCHAR(50),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    data JSONB, -- Stocké en JSONB pour Postgres, mappé en String coté Java si besoin
    CONSTRAINT fk_history_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_notif_user ON notifications(user_id);

-- 7. Table OFFERS (La demande)
CREATE TABLE IF NOT EXISTS offers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    passenger_id UUID NOT NULL,
    selected_driver_id UUID,
    start_point TEXT,
    end_point TEXT,
    price DECIMAL(10, 2),
    state offer_state_enum DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_offers_passenger FOREIGN KEY (passenger_id) REFERENCES users(id),
    CONSTRAINT fk_offers_driver FOREIGN KEY (selected_driver_id) REFERENCES drivers(id)
);

-- 8. Table LINKAGE (Qui a postulé à quoi)
CREATE TABLE IF NOT EXISTS offer_driver_linkages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    offer_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_link_offer FOREIGN KEY (offer_id) REFERENCES offers(id) ON DELETE CASCADE,
    CONSTRAINT fk_link_driver FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE CASCADE,
    CONSTRAINT uk_offer_driver UNIQUE (offer_id, driver_id)
);

-- 9. Table RIDES (La course validée)
CREATE TABLE IF NOT EXISTS rides (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    offer_id UUID UNIQUE, -- 1 Offre = 1 Course max
    passenger_id UUID,
    driver_id UUID,
    distance DECIMAL(10,2),
    time_estimation INT, -- en minutes
    real_time INT,
    state ride_state_enum DEFAULT 'CREATED',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_rides_offer FOREIGN KEY (offer_id) REFERENCES offers(id),
    CONSTRAINT fk_rides_passenger FOREIGN KEY (passenger_id) REFERENCES users(id),
    CONSTRAINT fk_rides_driver FOREIGN KEY (driver_id) REFERENCES drivers(id)
);

-- 10. Tables RBAC (Gestion locale simple des rôles)
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_has_roles (
    user_id UUID,
    role_id UUID,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_uhr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_uhr_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS role_has_permissions (
    role_id UUID,
    permission_id UUID,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rhp_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rhp_perm FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_has_permissions (
    user_id UUID,
    permission_id UUID,
    PRIMARY KEY (user_id, permission_id),
    CONSTRAINT fk_uhp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_uhp_perm FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);