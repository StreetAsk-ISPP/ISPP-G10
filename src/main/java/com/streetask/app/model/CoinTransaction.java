package com.streetask.app.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.streetask.app.model.enums.CoinTransactionStatus;
import com.streetask.app.model.enums.CoinTransactionType;
import com.streetask.app.user.RegularUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "coin_transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_coin_transactions_user_idempotency", columnNames = { "user_id",
                "idempotency_key" }),
        @UniqueConstraint(name = "uk_coin_transactions_external_payment", columnNames = { "external_payment_id" })
})
@Getter
@Setter
public class CoinTransaction extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private RegularUser user;

    @Enumerated(EnumType.STRING)
    private CoinTransactionType type;

    private Integer amount;

    @Column(length = 32)
    private String currency;

    @Enumerated(EnumType.STRING)
    private CoinTransactionStatus status;

    private Integer balanceBefore;

    private Integer balanceAfter;

    private UUID referenceId;

    @Column(name = "external_payment_id")
    private String externalPaymentId;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    private String description;

    private LocalDateTime createdAt;
}
