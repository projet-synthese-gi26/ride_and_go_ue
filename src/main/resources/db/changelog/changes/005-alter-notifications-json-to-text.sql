-- liquibase formatted sql

-- changeset yowyob:005-alter-notifications-data-type
-- comment: Changement du type de la colonne data de JSONB à TEXT pour compatibilité R2DBC simple

ALTER TABLE ride_and_go.notifications 
ALTER COLUMN data TYPE TEXT;