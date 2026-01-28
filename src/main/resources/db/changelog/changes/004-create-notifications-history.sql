-- liquibase formatted sql

-- changeset yowyob:004-create-notifications-history
-- comment: Table pour l'historique des notifications in-app

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(255),
    message TEXT,
    type VARCHAR(50), -- INFO, WARNING, SUCCESS
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    data JSONB, -- Pour stocker les métadonnées (offerId, price, etc.)
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notif_user_date ON notifications(user_id, created_at DESC);