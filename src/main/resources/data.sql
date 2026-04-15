-- Authorities seed data
-- MySQL/Aiven manual script in DBeaver:
-- INSERT INTO authorities(id, authority) VALUES (UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), 'ADMIN');
-- INSERT INTO authorities(id, authority) VALUES (UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), 'USER');
-- INSERT INTO authorities(id, authority) VALUES (UUID_TO_BIN('33333333-3333-3333-3333-333333333333'), 'BUSINESS');
INSERT INTO authorities(id, authority) VALUES ('11111111-1111-1111-1111-111111111111', 'ADMIN');
INSERT INTO authorities(id, authority) VALUES ('22222222-2222-2222-2222-222222222222', 'USER');
INSERT INTO authorities(id, authority) VALUES ('33333333-3333-3333-3333-333333333333', 'BUSINESS');

-- Admin user: admin1 / password: 4dm1n
-- MySQL/Aiven manual script in DBeaver:
-- INSERT INTO appusers(id, email, user_name, password, first_name, last_name, authority)
-- VALUES (
--     UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
--     'admin1@streetask.com',
--     'admin1',
--     '$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',
--     'Admin',
--     'User',
--     UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
-- );
INSERT INTO appusers(id, email, user_name, password, first_name, last_name, authority)
VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'admin1@streetask.com',
    'admin1',
    '$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',
    'Admin',
    'User',
    '11111111-1111-1111-1111-111111111111'
);

-- Admin profile row required by JOINED inheritance (User -> Admin)
INSERT INTO admins (id, role, permissions, assigned_at)
VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'SUPER_ADMIN',
    '{"canModerate": true, "canManageUsers": true}',
    CURRENT_TIMESTAMP
);

-- Regular user: user1@streetask.com / password: 4dm1n
-- MySQL/Aiven manual script in DBeaver:
-- INSERT INTO appusers (id, email, user_name, password, first_name, last_name, authority, account_type, active, created_at)
-- VALUES (
--     UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
--     'user1@streetask.com',
--     'user1',
--     '$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',
--     'Regular',
--     'User',
--     UUID_TO_BIN('22222222-2222-2222-2222-222222222222'),
--     'REGULAR_USER',
--     TRUE,
--     CURRENT_TIMESTAMP
-- );
INSERT INTO appusers (id, email, user_name, password, first_name, last_name, authority, account_type, active, created_at)
VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'user1@streetask.com',
    'user1',
    '$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',
    'Regular',
    'User',
    '22222222-2222-2222-2222-222222222222',
    'REGULAR_USER',
    TRUE,
    CURRENT_TIMESTAMP
);

-- RegularUser profile for user1
-- MySQL/Aiven manual script in DBeaver:
-- INSERT INTO regular_users (id, coin_balance, rating, verified, visibility_radius_km, phone, profile_photo)
-- VALUES (
--     UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
--     0,
--     0,
--     FALSE,
--     10,
--     '123456789',
--     NULL
-- );
INSERT INTO regular_users (id, coin_balance, rating, total_likes_received, total_dislikes_received, verified, visibility_radius_km, phone, profile_photo)
VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    0,
    0,
    0,
    0,
    FALSE,
    10,
    '123456789',
    NULL
);

-- Premium regular user: premium1@streetask.com / password: 4dm1n
INSERT INTO appusers (id, email, user_name, password, first_name, last_name, authority, account_type, active, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    'premium1@streetask.com',
    'premium1',
    '$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',
    'Premium',
    'User',
    '22222222-2222-2222-2222-222222222222',
    'REGULAR_USER',
    TRUE,
    CURRENT_TIMESTAMP
);

-- RegularUser profile for premium1 (premium_active = TRUE)
INSERT INTO regular_users (id, coin_balance, rating, total_likes_received, total_dislikes_received, verified, visibility_radius_km, phone, profile_photo, premium_active)
VALUES (
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    0,
    0,
    0,
    0,
    FALSE,
    10,
    '987654321',
    NULL,
    TRUE
);

-- Featured questions in Seville (permanent, never expire)
-- Centro (Plaza Nueva)
INSERT INTO questions (id, creator_id, event_id, title, content, latitude, longitude, radius_km, active, featured, expires_at, created_at, answer_count)
VALUES ('dd000000-0000-0000-0000-000000000001', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NULL, 'What is the best night plan in Seville? 🍻', 'Cocktail bars, terraces, flamenco shows... what is the real Seville nightlife like?', 37.3891, -5.9847, 50.0, TRUE, TRUE, '2099-12-31 23:59:59', CURRENT_TIMESTAMP, 0);
-- Estadio Benito Villamarín (Betis)
INSERT INTO questions (id, creator_id, event_id, title, content, latitude, longitude, radius_km, active, featured, expires_at, created_at, answer_count)
VALUES ('dd000000-0000-0000-0000-000000000002', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NULL, 'Betis or Sevilla? No shame 😏', 'You are in front of Villamarin. There is no middle ground here. Which side are you on?', 37.3564, -5.9812, 50.0, TRUE, TRUE, '2099-12-31 23:59:59', CURRENT_TIMESTAMP, 0);
-- Reina Mercedes
INSERT INTO questions (id, creator_id, event_id, title, content, latitude, longitude, radius_km, active, featured, expires_at, created_at, answer_count)
VALUES ('dd000000-0000-0000-0000-000000000003', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NULL, 'Do you like someone at Reina? 😳', 'Come on, confess... is there someone on campus that makes you nervous every time you see them? 👀', 37.3580, -5.9866, 50.0, TRUE, TRUE, '2099-12-31 23:59:59', CURRENT_TIMESTAMP, 0);
-- Triana
INSERT INTO questions (id, creator_id, event_id, title, content, latitude, longitude, radius_km, active, featured, expires_at, created_at, answer_count)
VALUES ('dd000000-0000-0000-0000-000000000004', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NULL, 'Who is going out partying tonight? 🎉', 'Do you have plans tonight or are you staying home? Come on and tell us...', 37.3840, -6.0028, 50.0, TRUE, TRUE, '2099-12-31 23:59:59', CURRENT_TIMESTAMP, 0);

