-- Seed business accounts for upcoming implementation tests.
-- Deterministic UUIDs and upserts keep this migration idempotent.

-- business01 / password: 4dm1n
INSERT INTO appusers (
    id,
    email,
    user_name,
    password,
    first_name,
    last_name,
    authority,
    account_type,
    active,
    created_at
)
VALUES (
    UUID_TO_BIN('d1111111-1111-1111-1111-111111111111'),
    'business01@streetask.com',
    'business01',
    '$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',
    'Business',
    'One',
    UUID_TO_BIN('33333333-3333-3333-3333-333333333333'),
    'BUSINESS',
    FALSE,
    CURRENT_TIMESTAMP
)
AS incoming
ON DUPLICATE KEY UPDATE
    email = incoming.email,
    user_name = incoming.user_name,
    password = incoming.password,
    first_name = incoming.first_name,
    last_name = incoming.last_name,
    authority = incoming.authority,
    account_type = incoming.account_type,
    active = incoming.active;

INSERT INTO business_accounts (
    id,
    company_name,
    tax_id,
    address,
    description,
    website,
    request_status,
    verified,
    subscription_active,
    verified_at,
    verified_by_id,
    subscription_expires_at,
    rating,
    logo,
    rejection_reason
)
VALUES (
    UUID_TO_BIN('d1111111-1111-1111-1111-111111111111'),
    'StreetAsk Business One',
    'B90000001',
    'Gran Via 10, Sevilla',
    'Test business account 1',
    'https://business01.streetask.test',
    'PENDING',
    FALSE,
    FALSE,
    NULL,
    NULL,
    NULL,
    0,
    NULL,
    NULL
)
AS incoming
ON DUPLICATE KEY UPDATE
    company_name = incoming.company_name,
    tax_id = incoming.tax_id,
    address = incoming.address,
    description = incoming.description,
    website = incoming.website,
    request_status = incoming.request_status,
    verified = incoming.verified,
    subscription_active = incoming.subscription_active,
    verified_at = incoming.verified_at,
    verified_by_id = incoming.verified_by_id,
    subscription_expires_at = incoming.subscription_expires_at,
    rating = incoming.rating,
    logo = incoming.logo,
    rejection_reason = incoming.rejection_reason;

-- business02 / password: 4dm1n
INSERT INTO appusers (
    id,
    email,
    user_name,
    password,
    first_name,
    last_name,
    authority,
    account_type,
    active,
    created_at
)
VALUES (
    UUID_TO_BIN('d2222222-2222-2222-2222-222222222222'),
    'business02@streetask.com',
    'business02',
    '$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',
    'Business',
    'Two',
    UUID_TO_BIN('33333333-3333-3333-3333-333333333333'),
    'BUSINESS',
    TRUE,
    CURRENT_TIMESTAMP
)
AS incoming
ON DUPLICATE KEY UPDATE
    email = incoming.email,
    user_name = incoming.user_name,
    password = incoming.password,
    first_name = incoming.first_name,
    last_name = incoming.last_name,
    authority = incoming.authority,
    account_type = incoming.account_type,
    active = incoming.active;

INSERT INTO business_accounts (
    id,
    company_name,
    tax_id,
    address,
    description,
    website,
    request_status,
    verified,
    subscription_active,
    verified_at,
    verified_by_id,
    subscription_expires_at,
    rating,
    logo,
    rejection_reason
)
VALUES (
    UUID_TO_BIN('d2222222-2222-2222-2222-222222222222'),
    'StreetAsk Business Two',
    'B90000002',
    'Avenida de la Constitucion 15, Sevilla',
    'Test business account 2',
    'https://business02.streetask.test',
    'APPROVED',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY),
    4.8,
    NULL,
    NULL
)
AS incoming
ON DUPLICATE KEY UPDATE
    company_name = incoming.company_name,
    tax_id = incoming.tax_id,
    address = incoming.address,
    description = incoming.description,
    website = incoming.website,
    request_status = incoming.request_status,
    verified = incoming.verified,
    subscription_active = incoming.subscription_active,
    verified_at = incoming.verified_at,
    verified_by_id = incoming.verified_by_id,
    subscription_expires_at = incoming.subscription_expires_at,
    rating = incoming.rating,
    logo = incoming.logo,
    rejection_reason = incoming.rejection_reason;

-- business03 / password: 4dm1n
INSERT INTO appusers (
    id,
    email,
    user_name,
    password,
    first_name,
    last_name,
    authority,
    account_type,
    active,
    created_at
)
VALUES (
    UUID_TO_BIN('d3333333-3333-3333-3333-333333333333'),
    'business03@streetask.com',
    'business03',
    '$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',
    'Business',
    'Three',
    UUID_TO_BIN('33333333-3333-3333-3333-333333333333'),
    'BUSINESS',
    TRUE,
    CURRENT_TIMESTAMP
)
AS incoming
ON DUPLICATE KEY UPDATE
    email = incoming.email,
    user_name = incoming.user_name,
    password = incoming.password,
    first_name = incoming.first_name,
    last_name = incoming.last_name,
    authority = incoming.authority,
    account_type = incoming.account_type,
    active = incoming.active;

INSERT INTO business_accounts (
    id,
    company_name,
    tax_id,
    address,
    description,
    website,
    request_status,
    verified,
    subscription_active,
    verified_at,
    verified_by_id,
    subscription_expires_at,
    rating,
    logo,
    rejection_reason
)
VALUES (
    UUID_TO_BIN('d3333333-3333-3333-3333-333333333333'),
    'StreetAsk Business Three',
    'B90000003',
    'Calle Sierpes 22, Sevilla',
    'Test business account 3',
    'https://business03.streetask.test',
    'APPROVED',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    NULL,
    3.9,
    NULL,
    NULL
)
AS incoming
ON DUPLICATE KEY UPDATE
    company_name = incoming.company_name,
    tax_id = incoming.tax_id,
    address = incoming.address,
    description = incoming.description,
    website = incoming.website,
    request_status = incoming.request_status,
    verified = incoming.verified,
    subscription_active = incoming.subscription_active,
    verified_at = incoming.verified_at,
    verified_by_id = incoming.verified_by_id,
    subscription_expires_at = incoming.subscription_expires_at,
    rating = incoming.rating,
    logo = incoming.logo,
    rejection_reason = incoming.rejection_reason;

-- business04 / password: 4dm1n
INSERT INTO appusers (
    id,
    email,
    user_name,
    password,
    first_name,
    last_name,
    authority,
    account_type,
    active,
    created_at
)
VALUES (
    UUID_TO_BIN('d4444444-4444-4444-4444-444444444444'),
    'business04@streetask.com',
    'business04',
    '$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',
    'Business',
    'Four',
    UUID_TO_BIN('33333333-3333-3333-3333-333333333333'),
    'BUSINESS',
    FALSE,
    CURRENT_TIMESTAMP
)
AS incoming
ON DUPLICATE KEY UPDATE
    email = incoming.email,
    user_name = incoming.user_name,
    password = incoming.password,
    first_name = incoming.first_name,
    last_name = incoming.last_name,
    authority = incoming.authority,
    account_type = incoming.account_type,
    active = incoming.active;

INSERT INTO business_accounts (
    id,
    company_name,
    tax_id,
    address,
    description,
    website,
    request_status,
    verified,
    subscription_active,
    verified_at,
    verified_by_id,
    subscription_expires_at,
    rating,
    logo,
    rejection_reason
)
VALUES (
    UUID_TO_BIN('d4444444-4444-4444-4444-444444444444'),
    'StreetAsk Business Four',
    'B90000004',
    'Calle Betis 18, Sevilla',
    'Test business account 4',
    'https://business04.streetask.test',
    'REJECTED',
    FALSE,
    FALSE,
    NULL,
    UUID_TO_BIN('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    NULL,
    1.7,
    NULL,
    'Missing fiscal documentation'
)
AS incoming
ON DUPLICATE KEY UPDATE
    company_name = incoming.company_name,
    tax_id = incoming.tax_id,
    address = incoming.address,
    description = incoming.description,
    website = incoming.website,
    request_status = incoming.request_status,
    verified = incoming.verified,
    subscription_active = incoming.subscription_active,
    verified_at = incoming.verified_at,
    verified_by_id = incoming.verified_by_id,
    subscription_expires_at = incoming.subscription_expires_at,
    rating = incoming.rating,
    logo = incoming.logo,
    rejection_reason = incoming.rejection_reason;

-- business05 / password: 4dm1n
INSERT INTO appusers (
    id,
    email,
    user_name,
    password,
    first_name,
    last_name,
    authority,
    account_type,
    active,
    created_at
)
VALUES (
    UUID_TO_BIN('d5555555-5555-5555-5555-555555555555'),
    'business05@streetask.com',
    'business05',
    '$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',
    'Business',
    'Five',
    UUID_TO_BIN('33333333-3333-3333-3333-333333333333'),
    'BUSINESS',
    FALSE,
    CURRENT_TIMESTAMP
)
AS incoming
ON DUPLICATE KEY UPDATE
    email = incoming.email,
    user_name = incoming.user_name,
    password = incoming.password,
    first_name = incoming.first_name,
    last_name = incoming.last_name,
    authority = incoming.authority,
    account_type = incoming.account_type,
    active = incoming.active;

INSERT INTO business_accounts (
    id,
    company_name,
    tax_id,
    address,
    description,
    website,
    request_status,
    verified,
    subscription_active,
    verified_at,
    verified_by_id,
    subscription_expires_at,
    rating,
    logo,
    rejection_reason
)
VALUES (
    UUID_TO_BIN('d5555555-5555-5555-5555-555555555555'),
    'StreetAsk Business Five',
    'B90000005',
    'Calle Feria 33, Sevilla',
    'Test business account 5 (new request)',
    'https://business05.streetask.test',
    'PENDING',
    FALSE,
    FALSE,
    NULL,
    NULL,
    NULL,
    2.5,
    NULL,
    NULL
)
AS incoming
ON DUPLICATE KEY UPDATE
    company_name = incoming.company_name,
    tax_id = incoming.tax_id,
    address = incoming.address,
    description = incoming.description,
    website = incoming.website,
    request_status = incoming.request_status,
    verified = incoming.verified,
    subscription_active = incoming.subscription_active,
    verified_at = incoming.verified_at,
    verified_by_id = incoming.verified_by_id,
    subscription_expires_at = incoming.subscription_expires_at,
    rating = incoming.rating,
    logo = incoming.logo,
    rejection_reason = incoming.rejection_reason;