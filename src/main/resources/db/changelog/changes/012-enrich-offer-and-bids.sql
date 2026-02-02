-- liquibase formatted sql

-- changeset yowyob:012-enrich-offer-and-bids
ALTER TABLE ride_and_go.offers ADD COLUMN passenger_phone VARCHAR(50);
ALTER TABLE ride_and_go.offers ADD COLUMN departure_time VARCHAR(50);