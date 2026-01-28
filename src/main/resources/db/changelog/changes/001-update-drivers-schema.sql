--- FILE: src/main/resources/db/changelog/changes/001-update-drivers-schema.sql ---
-- liquibase formatted sql

-- changeset yowyob:001-add-driver-attributes
-- comment: Ajout des attributs de statut et profil pour les chauffeurs

ALTER TABLE drivers 
ADD COLUMN IF NOT EXISTS is_online BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS is_profile_completed BOOLEAN DEFAULT FALSE;

-- On s'assure que status est bien présent (au cas où on part d'une base vide)
-- Ceci n'est nécessaire que si la table drivers n'a pas été créée correctement par schema.sql auparavant
-- Dans un vrai projet avec Liquibase, TOUT le schema.sql devrait être converti en changelogs.
-- Pour l'instant, on se contente d'altérer l'existant.