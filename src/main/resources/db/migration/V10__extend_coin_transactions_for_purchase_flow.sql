ALTER TABLE `coin_transactions`
    ADD COLUMN `currency` VARCHAR(32) DEFAULT NULL,
    ADD COLUMN `status` ENUM('PENDING', 'SUCCESS', 'FAILED') DEFAULT NULL,
    ADD COLUMN `external_payment_id` VARCHAR(255) DEFAULT NULL,
    ADD COLUMN `idempotency_key` VARCHAR(255) DEFAULT NULL,
    ADD COLUMN `description` VARCHAR(255) DEFAULT NULL;

ALTER TABLE `coin_transactions`
    MODIFY COLUMN `type` ENUM('EARN', 'SPEND', 'PURCHASE') DEFAULT NULL;

CREATE UNIQUE INDEX `uk_coin_transactions_external_payment`
    ON `coin_transactions` (`external_payment_id`);

CREATE UNIQUE INDEX `uk_coin_transactions_user_idempotency`
    ON `coin_transactions` (`user_id`, `idempotency_key`);

UPDATE `coin_transactions`
SET `currency` = 'StreetCoins'
WHERE `currency` IS NULL;

UPDATE `coin_transactions`
SET `status` = 'SUCCESS'
WHERE `status` IS NULL;
