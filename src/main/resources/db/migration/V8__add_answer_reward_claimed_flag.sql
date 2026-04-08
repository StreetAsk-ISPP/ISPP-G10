ALTER TABLE answers
    ADD COLUMN reward_claimed BIT(1) NOT NULL DEFAULT b'0';

UPDATE answers
SET reward_claimed = b'0'
WHERE reward_claimed IS NULL;
