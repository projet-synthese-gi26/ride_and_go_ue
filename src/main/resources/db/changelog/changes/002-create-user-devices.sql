-- liquibase formatted sql

-- changeset yowyob:002-create-user-devices
-- comment: Table pour stocker les tokens FCM/APNS des utilisateurs pour le Push

CREATE TABLE IF NOT EXISTS user_devices (
    user_id UUID PRIMARY KEY,
    device_token TEXT NOT NULL,
    platform VARCHAR(20), -- ANDROID, IOS, WEB
    last_updated_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_user_device_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);