-- liquibase formatted sql
-- changeset yowyob:010-create-reviews
-- comment: Création de la table des avis et enrichissement de la table drivers
CREATE TABLE IF NOT EXISTS ride_and_go.reviews (
    id UUID PRIMARY KEY,
    ride_id UUID NOT NULL,
    passenger_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    rating INTEGER NOT NULL CHECK (
        rating >= 1
        AND rating <= 5
    ),
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_review_ride FOREIGN KEY (ride_id) REFERENCES ride_and_go.rides(id),
    CONSTRAINT uk_review_ride UNIQUE (ride_id)
);

-- Ajout des champs de score au chauffeur
ALTER TABLE
    ride_and_go.drivers
ADD
    COLUMN IF NOT EXISTS rating DECIMAL(3, 2) DEFAULT 0.0;

ALTER TABLE
    ride_and_go.drivers
ADD
    COLUMN IF NOT EXISTS total_reviews_count INTEGER DEFAULT 0;