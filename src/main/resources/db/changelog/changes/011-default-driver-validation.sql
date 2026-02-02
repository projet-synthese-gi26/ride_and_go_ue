-- liquibase formatted sql

-- changeset yowyob:011-default-driver-validation
-- comment: Définit la validation du profil à TRUE par défaut 

-- 1. Modifier la valeur par défaut pour les futures insertions
ALTER TABLE ride_and_go.drivers 
ALTER COLUMN is_profile_validated SET DEFAULT TRUE;

-- 2. Mettre à jour les enregistrements existants 
UPDATE ride_and_go.drivers 
SET is_profile_validated = TRUE 
WHERE is_profile_validated IS FALSE;