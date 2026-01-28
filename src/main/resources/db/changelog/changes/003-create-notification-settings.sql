-- liquibase formatted sql

-- changeset yowyob:003-create-notification-settings
-- comment: Table pour stocker les préférences de notification des utilisateurs

CREATE TABLE IF NOT EXISTS notification_settings (
    user_id UUID PRIMARY KEY,
    enable_email BOOLEAN DEFAULT TRUE,
    enable_sms BOOLEAN DEFAULT FALSE,
    enable_push BOOLEAN DEFAULT TRUE,
    enable_whatsapp BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_notif_settings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);