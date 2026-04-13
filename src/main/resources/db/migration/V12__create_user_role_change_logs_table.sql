CREATE TABLE IF NOT EXISTS `user_role_change_logs` (
  `changed_at` datetime(6) NOT NULL,
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `previous_account_type` varchar(50) NOT NULL,
  `new_account_type` varchar(50) NOT NULL,
  `previous_authority` varchar(50) NOT NULL,
  `new_authority` varchar(50) NOT NULL,
  `changed_by` varchar(255) DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `ip_address` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_role_change_logs_user_id` (`user_id`),
  CONSTRAINT `fk_user_role_change_logs_user` FOREIGN KEY (`user_id`) REFERENCES `appusers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
