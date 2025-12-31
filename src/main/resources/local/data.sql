-- =====================================================
-- YOWYOB DB - DATA SEEDING SCRIPT
-- PostgreSQL
-- =====================================================

-- =====================================================
-- 1. CLEANUP EXISTING DATA
-- =====================================================
TRUNCATE TABLE 
    users, images, countries, business_actors, 
    admins, fleet_managers, drivers, customers, 
    providers, employees, prospects, sales_persons,
    profiles, settings, addresses, contacts,
    roles, permissions, role_has_permissions, user_has_permissions, user_has_roles,
    organizations, agencies, business_domains, organization_business_domains,
    certifications, third_parties, proposed_activities, services, branches,
    fleets, vehicles, geofence_zones, geofence_points, geofence_events,
    roads, trips, offers, rides, reviews, offer_driver_linkages,
    operational_parameters, financial_parameters, maintenance_parameters,
    geofence_point_zone_linkages,
    syndicats, abstract_products, products, 
    publications, publication_images, publication_votes, votes,
    events, event_images, reactions, comments, avis,
    subscriptions, payments
    RESTART IDENTITY CASCADE;

-- =====================================================
-- 2. STATIC REFERENCE DATA
-- =====================================================

-- Countries
INSERT INTO countries (name, code) VALUES
('Cameroun', 'CM'), ('Sénégal', 'SN'), ('Côte d''Ivoire', 'CI'), 
('Gabon', 'GA'), ('Nigéria', 'NG'), ('Togo', 'TG');

-- Roles
INSERT INTO roles (name, guard_name) VALUES
('ADMIN', 'web'), ('FLEET_MANAGER', 'web'), ('DRIVER', 'web'), ('CUSTOMER', 'web');

-- Images
INSERT INTO images (url, alt_text) 
SELECT 
    'https://picsum.photos/seed/' || i || '/800/600', 
    'Image aléatoire ' || i
FROM generate_series(1, 50) as i;

-- =====================================================
-- 3. USERS GENERATION
-- =====================================================

-- 3.1 GOLDEN USERS (Pour les tests Swagger avec Tokens fixes)
-- Client (7f13909e...)
INSERT INTO users (id, name, phone_number, email_address) VALUES 
('7f13909e-7170-4f91-872e-333333333333', 'Test Client', '+237600000001', 'client@test.com');

-- Driver 1 (a1b2c3d4...1111)
INSERT INTO users (id, name, phone_number, email_address) VALUES 
('a1b2c3d4-e5f6-4a5b-8c9d-111111111111', 'Eto''o Fils', '+237600000002', 'etoo@test.com');

-- Driver 2 (a1b2c3d4...2222)
INSERT INTO users (id, name, phone_number, email_address) VALUES 
('a1b2c3d4-e5f6-4a5b-8c9d-222222222222', 'Aboubakar V', '+237600000003', 'aboubakar@test.com');


-- 3.2 RANDOM USERS (97 autres pour faire masse)
INSERT INTO users (name, phone_number, email_address)
SELECT 
    (ARRAY['Mamadou', 'Jean-Pierre', 'Ibrahim', 'Fatou', 'Aminata', 'Ngolo', 'Clarisse', 'Samuel', 'Kouamé', 'Aissatou', 'Bachelard', 'Landry', 'Thérèse'])[floor(random()*13)+1] 
    || ' ' || 
    (ARRAY['Diop', 'Njoya', 'Mbarga', 'Kone', 'Sow', 'Etoundi', 'Kamga', 'Traoré', 'Diallo', 'Fofana', 'Mensah', 'Atangana', 'Drogba'])[floor(random()*13)+1],
    (ARRAY['+237', '+221', '+225'])[floor(random()*3)+1] || (600000000 + floor(random()*99999999)::int),
    'user_' || i || '@yowyob.test'
FROM generate_series(1, 97) as i;

-- Settings & Profiles
INSERT INTO settings (user_id, theme, language, receive_push_notifications)
SELECT id, 'LIGHT', 'fr', true FROM users;

INSERT INTO profiles (user_id, first_name, nationality, is_verified)
SELECT id, split_part(name, ' ', 1), 'Camerounais', (random() > 0.5) FROM users;

-- =====================================================
-- 4. ACTORS DISPATCHING
-- =====================================================

-- 4.1 Golden Users Dispatching
-- Client -> Customer
INSERT INTO customers (id, code, payment_method) VALUES 
('7f13909e-7170-4f91-872e-333333333333', 'CUST-TEST', 'CASH');

-- Drivers -> BusinessActor + Driver
INSERT INTO business_actors (id, name, phone_number, email_address) VALUES 
('a1b2c3d4-e5f6-4a5b-8c9d-111111111111', 'Eto''o Fils', '+237600000002', 'etoo@test.com'),
('a1b2c3d4-e5f6-4a5b-8c9d-222222222222', 'Aboubakar V', '+237600000003', 'aboubakar@test.com');

INSERT INTO drivers (id, status, license_number, has_car) VALUES 
('a1b2c3d4-e5f6-4a5b-8c9d-111111111111', 'AVAILABLE', 'LIC-ETOO-09', true),
('a1b2c3d4-e5f6-4a5b-8c9d-222222222222', 'AVAILABLE', 'LIC-ABOU-10', true);

-- Assign Roles for Golden Users
INSERT INTO user_has_roles (user_id, role_id) VALUES 
('7f13909e-7170-4f91-872e-333333333333', (SELECT id FROM roles WHERE name = 'CUSTOMER')),
('a1b2c3d4-e5f6-4a5b-8c9d-111111111111', (SELECT id FROM roles WHERE name = 'DRIVER')),
('a1b2c3d4-e5f6-4a5b-8c9d-222222222222', (SELECT id FROM roles WHERE name = 'DRIVER'));


-- 4.2 Random Users Dispatching
-- Admins (Next 5)
INSERT INTO admins (id, name, email_address)
SELECT id, name, email_address FROM users 
WHERE id NOT IN ('7f13909e-7170-4f91-872e-333333333333', 'a1b2c3d4-e5f6-4a5b-8c9d-111111111111', 'a1b2c3d4-e5f6-4a5b-8c9d-222222222222')
ORDER BY email_address LIMIT 5;

-- Fleet Managers (Next 5)
INSERT INTO business_actors (id, name, phone_number, email_address)
SELECT id, name, phone_number, email_address FROM users 
WHERE id NOT IN ('7f13909e-7170-4f91-872e-333333333333', 'a1b2c3d4-e5f6-4a5b-8c9d-111111111111', 'a1b2c3d4-e5f6-4a5b-8c9d-222222222222')
ORDER BY email_address LIMIT 5 OFFSET 5;

INSERT INTO fleet_managers (id, name, email_address)
SELECT id, name, email_address FROM users 
WHERE id NOT IN ('7f13909e-7170-4f91-872e-333333333333', 'a1b2c3d4-e5f6-4a5b-8c9d-111111111111', 'a1b2c3d4-e5f6-4a5b-8c9d-222222222222')
ORDER BY email_address LIMIT 5 OFFSET 5;

-- Drivers (Next 30)
INSERT INTO business_actors (id, name, phone_number, email_address)
SELECT id, name, phone_number, email_address FROM users 
WHERE id NOT IN ('7f13909e-7170-4f91-872e-333333333333', 'a1b2c3d4-e5f6-4a5b-8c9d-111111111111', 'a1b2c3d4-e5f6-4a5b-8c9d-222222222222')
ORDER BY email_address LIMIT 30 OFFSET 10;

INSERT INTO drivers (id, status, license_number, has_car)
SELECT 
    id, 
    (ARRAY['AVAILABLE', 'BUSY', 'OFFLINE'])[floor(random()*3)+1], 
    'LIC-' || floor(random()*100000) || '-CM',
    (random() > 0.3)
FROM users 
WHERE id NOT IN ('7f13909e-7170-4f91-872e-333333333333', 'a1b2c3d4-e5f6-4a5b-8c9d-111111111111', 'a1b2c3d4-e5f6-4a5b-8c9d-222222222222')
ORDER BY email_address LIMIT 30 OFFSET 10;

-- Customers (Remaining)
INSERT INTO customers (id, code, payment_method)
SELECT 
    id, 
    'CUST-' || substr(id::text, 1, 8),
    (ARRAY['CASH', 'MOBILE_MONEY', 'CARD'])[floor(random()*3)+1]
FROM users 
WHERE id NOT IN ('7f13909e-7170-4f91-872e-333333333333', 'a1b2c3d4-e5f6-4a5b-8c9d-111111111111', 'a1b2c3d4-e5f6-4a5b-8c9d-222222222222')
AND id NOT IN (SELECT id FROM admins)
AND id NOT IN (SELECT id FROM fleet_managers)
AND id NOT IN (SELECT id FROM drivers);

-- Roles assignment (generic) omitted for brevity as handled above logic, 
-- but ensuring 'user_has_roles' is filled for everyone is good practice.

-- =====================================================
-- 5. ORGANIZATIONS & AGENCIES
-- =====================================================

INSERT INTO organizations (
    business_actor_id, logo_id, code, short_name, long_name, 
    description, tax_number, is_active, status
)
SELECT
    (SELECT id FROM fleet_managers ORDER BY random() LIMIT 1),
    (SELECT id FROM images ORDER BY random() LIMIT 1),
    'ORG-' || i,
    'Transport ' || i,
    'Société de Transport ' || i,
    'Description ' || i,
    'TAX-' || floor(random()*1000000),
    true,
    'PUBLISHED'
FROM generate_series(1, 35) as i;

INSERT INTO agencies (
    organization_id, manager_id, name, city, location, is_headquarter
)
SELECT 
    id, 
    (SELECT id FROM fleet_managers ORDER BY random() LIMIT 1),
    'Agence Centrale',
    (ARRAY['Douala', 'Yaoundé', 'Abidjan'])[floor(random()*3)+1],
    'Rue Principale',
    true
FROM organizations;

-- =====================================================
-- 6. FLEETS & VEHICLES
-- =====================================================

INSERT INTO fleets (fleet_manager_id, name, phone_number)
SELECT 
    id, 'Flotte de ' || name, '+237699000000'
FROM fleet_managers;

INSERT INTO vehicles (
    fleet_id, user_id, driver_id, license_plate, brand, model, type, color, manufacturing_year
)
SELECT 
    f.id,
    f.fleet_manager_id,
    (SELECT id FROM drivers ORDER BY random() LIMIT 1),
    'LT-' || floor(random()*999)::text || '-AA',
    (ARRAY['Toyota', 'Peugeot'])[floor(random()*2)+1],
    (ARRAY['Yaris', 'Partner'])[floor(random()*2)+1],
    (ARRAY['CAR', 'VAN'])[floor(random()*2)+1]::vehicle_type_enum,
    'Jaune',
    2020
FROM fleets f;

-- =====================================================
-- 7. OFFERS & RIDES (UPDATED)
-- =====================================================

INSERT INTO offers (
    passenger_id, start_point, end_point, price, state, created_at
)
SELECT 
    (SELECT id FROM customers ORDER BY random() LIMIT 1),
    'Point A', 'Point B', 
    2000, 
    'VALIDATED', -- UPDATE: 'CHOSEN' n'existe plus, remplacé par 'VALIDATED'
    NOW()
FROM generate_series(1, 60) as i;

-- Rides
INSERT INTO rides (
    offer_id, passenger_id, driver_id, 
    distance, time_estimation, real_time, state, created_at
)
SELECT 
    id, 
    passenger_id,
    (SELECT id FROM drivers ORDER BY random() LIMIT 1),
    15.5, 30, 35,
    'COMPLETED',
    created_at
FROM offers 
LIMIT 40;

-- Reviews
INSERT INTO reviews (ride_id, author_id, subject, comment, rating)
SELECT 
    id,
    passenger_id,
    'Chauffeur',
    'Bonne course, chauffeur ponctuel',
    5
FROM rides
WHERE state = 'COMPLETED';

-- =====================================================
-- 8. PRODUCTS
-- =====================================================
-- (Le reste est identique, l'état 'PUBLISHED' est valide pour status_enum)

INSERT INTO products (
    organization_id, name, description, standard_price, 
    status, is_active, departure_location
)
SELECT 
    (SELECT id FROM organizations ORDER BY random() LIMIT 1),
    'Trajet Spécial ' || i,
    'Description produit',
    5000,
    'PUBLISHED',
    true,
    'Douala'
FROM generate_series(1, 40) as i;

-- =====================================================
-- 9. SUBSCRIPTIONS & PAYMENTS
-- =====================================================

INSERT INTO subscriptions (
    admin_id, label, price, duration_in_days, description, is_active
) VALUES 
((SELECT id FROM admins LIMIT 1), 'Pack Hebdo', 2500, 7, 'Standard', true),
((SELECT id FROM admins LIMIT 1), 'Pack Mensuel', 10000, 30, 'Pro', true),
((SELECT id FROM admins LIMIT 1), 'Pack Annuel', 100000, 365, 'VIP', true);

INSERT INTO payments (
    driver_id, user_id, subscription_id, amount_paid, status, id_provider_transaction, created_at
)
SELECT 
    d.id, -- Driver ID
    d.id, -- Payer ID
    s.id, -- Subscription ID
    s.price,
    'SUCCESS',
    'TXN-' || floor(random()*10000000),
    NOW()
FROM drivers d
CROSS JOIN subscriptions s
ORDER BY random()
LIMIT 50;

-- =====================================================
-- 10. SOCIAL & ADDRESSES
-- =====================================================

INSERT INTO syndicats (organization_id, name, is_approved)
SELECT id, 'Syndicat Transports', true FROM organizations LIMIT 1;

INSERT INTO branches (id, syndicat_id, name, location)
SELECT 
    ag.id,
    (SELECT id FROM syndicats LIMIT 1),
    'Branche ' || ag.name, 
    ag.location 
FROM agencies ag 
LIMIT 10;

INSERT INTO publications (branch_id, author_id, content, status, n_likes)
SELECT 
    (SELECT id FROM branches ORDER BY random() LIMIT 1),
    (SELECT id FROM admins ORDER BY random() LIMIT 1),
    'Info Trafic ' || i,
    'PUBLISHED',
    floor(random() * 50)::int
FROM generate_series(1, 40) as i;

-- =====================================================
-- SEEDING DIRECT PERMISSIONS
-- =====================================================

INSERT INTO permissions (id, name) VALUES 
(uuid_generate_v4(), 'extra:special_access'),
(uuid_generate_v4(), 'fleet:emergency_stop');

INSERT INTO user_has_permissions (user_id, permission_id)
VALUES (
    (SELECT id FROM users ORDER BY email_address LIMIT 1),
    (SELECT id FROM permissions WHERE name = 'extra:special_access' LIMIT 1)
);