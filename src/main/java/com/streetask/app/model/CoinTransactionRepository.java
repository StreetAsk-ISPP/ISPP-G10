package com.streetask.app.model;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.streetask.app.model.enums.CoinTransactionType;

import jakarta.persistence.LockModeType;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {

    Optional<CoinTransaction> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    Optional<CoinTransaction> findByExternalPaymentId(String externalPaymentId);

    List<CoinTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<CoinTransaction> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, CoinTransactionType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tx from CoinTransaction tx where tx.id = :transactionId")
    Optional<CoinTransaction> findByIdForUpdate(@Param("transactionId") UUID transactionId);
}
