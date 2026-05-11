-- Normalize answer vote counters so legacy rows and business answers persist likes/dislikes correctly.
UPDATE answers
SET upvotes = COALESCE(upvotes, 0),
    downvotes = COALESCE(downvotes, 0);

ALTER TABLE answers
    MODIFY upvotes int NOT NULL DEFAULT 0,
    MODIFY downvotes int NOT NULL DEFAULT 0;