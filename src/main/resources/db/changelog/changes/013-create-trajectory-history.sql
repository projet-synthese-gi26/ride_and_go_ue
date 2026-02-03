-- liquibase formatted sql
-- changeset yowyob:013-create-trajectory-history
-- comment: Table pour l'historique massif des positions (Segments de trajets)

CREATE TABLE IF NOT EXISTS ride_and_go.driver_trajectory_history (
    id UUID PRIMARY KEY,
    driver_id UUID NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    points_count INTEGER,
    -- JSONB stockera le tableau [[lat, lon, ts], [lat, lon, ts], ...]
    trajectory_data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_trajectory_driver FOREIGN KEY (driver_id) REFERENCES ride_and_go.drivers(id) ON DELETE CASCADE
);

CREATE INDEX idx_trajectory_driver_time ON ride_and_go.driver_trajectory_history(driver_id, start_time DESC);