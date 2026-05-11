-- Add vote tracking columns to business_accounts
ALTER TABLE `business_accounts`
ADD COLUMN `total_likes_received` INT NOT NULL DEFAULT 0,
ADD COLUMN `total_dislikes_received` INT NOT NULL DEFAULT 0;

-- Update existing business accounts with vote counts from their answers
UPDATE `business_accounts` ba
SET ba.`total_likes_received` = COALESCE((
    SELECT SUM(a.`upvotes`)
    FROM `answers` a
    WHERE a.`user_id` = ba.`id`
), 0),
ba.`total_dislikes_received` = COALESCE((
    SELECT SUM(a.`downvotes`)
    FROM `answers` a
    WHERE a.`user_id` = ba.`id`
), 0);
