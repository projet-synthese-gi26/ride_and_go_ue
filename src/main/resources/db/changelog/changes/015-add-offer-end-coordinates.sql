-- liquibase formatted sql
-- changeset yowyob:015-add-offer-end-coordinates
-- comment: Ajout des coordonnées de départ et d'arrivée pour le trajet

ALTER TABLE ride_and_go.offers 
ADD COLUMN IF NOT EXISTS end_lat DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS end_lon DOUBLE PRECISION;