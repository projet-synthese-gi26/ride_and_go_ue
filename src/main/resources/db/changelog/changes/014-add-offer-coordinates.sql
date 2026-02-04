-- liquibase formatted sql
-- changeset yowyob:014-add-offer-coordinates
-- comment: Ajout des colonnes de géolocalisation pour le matching des offres

ALTER TABLE ride_and_go.offers 
ADD COLUMN IF NOT EXISTS start_lat DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS start_lon DOUBLE PRECISION;

-- Note: DOUBLE PRECISION en Postgres correspond au type Double en Java 