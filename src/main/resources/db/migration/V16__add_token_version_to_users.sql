-- Add tokenVersion column to support automatic token invalidation on role changes
-- This prevents users from using old tokens after their authority has changed

ALTER TABLE appusers
    ADD COLUMN token_version BIGINT DEFAULT 0 NOT NULL;

-- Create index for faster token version lookups during validation
CREATE INDEX idx_appusers_token_version ON appusers (token_version);
