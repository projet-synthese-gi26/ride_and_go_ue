-- liquibase formatted sql

-- changeset yowyob:006-update-drivers-vehicle
-- comment: Ajout de vehicle_id et is_profile_validated pour les chauffeurs

ALTER TABLE ride_and_go.drivers 
ADD COLUMN IF NOT EXISTS vehicle_id UUID,
ADD COLUMN IF NOT EXISTS is_profile_validated BOOLEAN DEFAULT FALSE;

-- On ne met pas de FK (Foreign Key) contrainte SQL car le véhicule est dans un autre microservice (autre DB potentiellement)
-- C'est une référence logique (Soft Reference).