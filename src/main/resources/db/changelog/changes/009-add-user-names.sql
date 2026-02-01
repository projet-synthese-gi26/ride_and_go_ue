-- liquibase formatted sql

-- changeset yowyob:009-add-user-names
-- comment: Ajout de first_name et last_name à la table users
ALTER TABLE ride_and_go.users ADD COLUMN first_name VARCHAR(255);
ALTER TABLE ride_and_go.users ADD COLUMN last_name VARCHAR(255);