package com.streetask.app.model;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {
}
