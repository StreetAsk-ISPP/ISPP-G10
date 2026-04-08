-- Add featured flag to questions and seed 4 permanent featured questions in Seville.

ALTER TABLE questions ADD COLUMN `featured` bit(1) DEFAULT b'0';

-- Seed user: uses user1 (bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb) as creator

-- Centro (Plaza Nueva)
INSERT INTO questions (id, creator_id, event_id, title, content, latitude, longitude, radius_km, active, featured, expires_at, created_at, answer_count)
VALUES (
    UUID_TO_BIN('dd000000-0000-0000-0000-000000000001'),
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    NULL,
    '¿Cuál es el mejor plan de noche en Sevilla? 🍻',
    'Bares de copas, terrazas, tablao flamenco... ¿cómo se vive la noche sevillana de verdad?',
    37.3891, -5.9847, 50.0, TRUE, TRUE, '2099-12-31 23:59:59', CURRENT_TIMESTAMP, 0
)
AS incoming
ON DUPLICATE KEY UPDATE title=incoming.title, content=incoming.content, latitude=incoming.latitude, longitude=incoming.longitude, featured=incoming.featured, active=incoming.active, expires_at=incoming.expires_at;

-- Estadio Benito Villamarín (Betis)
INSERT INTO questions (id, creator_id, event_id, title, content, latitude, longitude, radius_km, active, featured, expires_at, created_at, answer_count)
VALUES (
    UUID_TO_BIN('dd000000-0000-0000-0000-000000000002'),
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    NULL,
    '¿Betis o Sevilla? 😏',
    'Estás delante del Villamarín. Aquí no hay término medio. ¿De quién eres?',
    37.3564, -5.9812, 50.0, TRUE, TRUE, '2099-12-31 23:59:59', CURRENT_TIMESTAMP, 0
)
AS incoming
ON DUPLICATE KEY UPDATE title=incoming.title, content=incoming.content, latitude=incoming.latitude, longitude=incoming.longitude, featured=incoming.featured, active=incoming.active, expires_at=incoming.expires_at;

-- Reina Mercedes
INSERT INTO questions (id, creator_id, event_id, title, content, latitude, longitude, radius_km, active, featured, expires_at, created_at, answer_count)
VALUES (
    UUID_TO_BIN('dd000000-0000-0000-0000-000000000003'),
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    NULL,
    '¿Hay alguien en Reina que te guste? 😳',
    'Venga, confiesa... ¿hay alguien en el campus que te pone nervioso/a cada vez que lo/la ves? 👀',
    37.3580, -5.9866, 50.0, TRUE, TRUE, '2099-12-31 23:59:59', CURRENT_TIMESTAMP, 0
)
AS incoming
ON DUPLICATE KEY UPDATE title=incoming.title, content=incoming.content, latitude=incoming.latitude, longitude=incoming.longitude, featured=incoming.featured, active=incoming.active, expires_at=incoming.expires_at;

-- Triana
INSERT INTO questions (id, creator_id, event_id, title, content, latitude, longitude, radius_km, active, featured, expires_at, created_at, answer_count)
VALUES (
    UUID_TO_BIN('dd000000-0000-0000-0000-000000000004'),
    UUID_TO_BIN('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'),
    NULL,
    '¿Quién se va de fiesta hoy? 🎉',
    '¿Tienes planes esta noche o te quedas en casa? Anímate y cuéntanos...',
    37.3840, -6.0028, 50.0, TRUE, TRUE, '2099-12-31 23:59:59', CURRENT_TIMESTAMP, 0
)
AS incoming
ON DUPLICATE KEY UPDATE title=incoming.title, content=incoming.content, latitude=incoming.latitude, longitude=incoming.longitude, featured=incoming.featured, active=incoming.active, expires_at=incoming.expires_at;
