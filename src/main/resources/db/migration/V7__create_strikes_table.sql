CREATE TABLE IF NOT EXISTS `strikes` (
  `issued_at` datetime(6) DEFAULT NULL,
  `id` binary(16) NOT NULL,
  `issued_by_id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_strikes_issued_by` (`issued_by_id`),
  KEY `FK_strikes_user` (`user_id`),
  CONSTRAINT `FK_strikes_issued_by` FOREIGN KEY (`issued_by_id`) REFERENCES `appusers` (`id`),
  CONSTRAINT `FK_strikes_user` FOREIGN KEY (`user_id`) REFERENCES `regular_users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
