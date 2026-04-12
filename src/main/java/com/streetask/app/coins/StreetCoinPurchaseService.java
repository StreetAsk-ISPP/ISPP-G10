package com.streetask.app.coins;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.model.CoinTransaction;
import com.streetask.app.model.CoinTransactionRepository;
import com.streetask.app.model.enums.CoinTransactionStatus;
import com.streetask.app.model.enums.CoinTransactionType;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;
import com.streetask.app.user.User;
import com.streetask.app.user.UserService;

@Service
public class StreetCoinPurchaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreetCoinPurchaseService.class);
    private static final String STREET_COINS_CURRENCY = "StreetCoins";

    private final UserService userService;
    private final RegularUserRepository regularUserRepository;
    private final CoinTransactionRepository coinTransactionRepository;

    @Value("${streetask.coins.purchase.mode:stripe}")
    private String purchaseMode;

    @Value("${streetask.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${streetask.stripe.publishable-key:}")
    private String stripePublishableKey;

    @Value("${streetask.stripe.currency:eur}")
    private String stripeCurrency;

    @Value("${streetask.stripe.streetcoins-success-url:http://localhost:8081}")
    private String streetCoinsSuccessUrl;

    @Value("${streetask.stripe.streetcoins-cancel-url:http://localhost:8081}")
    private String streetCoinsCancelUrl;

    public StreetCoinPurchaseService(UserService userService,
            RegularUserRepository regularUserRepository,
            CoinTransactionRepository coinTransactionRepository) {
        this.userService = userService;
        this.regularUserRepository = regularUserRepository;
        this.coinTransactionRepository = coinTransactionRepository;
    }

    @Transactional(readOnly = true)
    public List<StreetCoinPackResponse> getAvailablePacks() {
        return List.of(StreetCoinPack.values()).stream()
                .map(pack -> new StreetCoinPackResponse(
                        pack.getId(),
                        pack.getLabel(),
                        pack.getAmountCents(),
                        pack.getAmountCents() / 100.0,
                        pack.getStreetCoins()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StreetCoinBalanceResponse getCurrentBalance() {
        RegularUser currentUser = requireCurrentRegularUser();
        return new StreetCoinBalanceResponse(currentUser.getId(), safeBalance(currentUser.getCoinBalance()),
                STREET_COINS_CURRENCY);
    }

    @Transactional(readOnly = true)
    public List<StreetCoinTransactionResponse> getCurrentTransactions() {
        RegularUser currentUser = requireCurrentRegularUser();

        return coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(tx -> new StreetCoinTransactionResponse(
                        tx.getId(),
                        tx.getAmount(),
                        resolveCurrency(tx),
                        resolveStatus(tx),
                        resolveDescription(tx),
                        tx.getExternalPaymentId(),
                        tx.getIdempotencyKey(),
                        tx.getCreatedAt()))
                .toList();
    }

    @Transactional
    public StreetCoinPurchaseResponse createPurchase(StreetCoinPurchaseRequest request, String idempotencyHeader) {
        RegularUser currentUser = requireCurrentRegularUser();
        String idempotencyKey = resolveIdempotencyKey(idempotencyHeader, request.getIdempotencyKey());

        Optional<CoinTransaction> existing = coinTransactionRepository
                .findByUserIdAndIdempotencyKey(currentUser.getId(), idempotencyKey);
        if (existing.isPresent()) {
            CoinTransaction existingTx = existing.get();
            return toPurchaseResponse(existingTx, resolveCheckoutUrl(existingTx));
        }

        StreetCoinPack selectedPack = StreetCoinPack.fromId(request.getPackId());

        CoinTransaction tx = new CoinTransaction();
        tx.setUser(currentUser);
        tx.setType(CoinTransactionType.PURCHASE);
        tx.setAmount(selectedPack.getStreetCoins());
        tx.setCurrency(STREET_COINS_CURRENCY);
        tx.setStatus(CoinTransactionStatus.PENDING);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setDescription(selectedPack.getLabel() + " - " + selectedPack.getStreetCoins() + " StreetCoins");
        tx.setCreatedAt(LocalDateTime.now());

        int currentBalance = safeBalance(currentUser.getCoinBalance());
        tx.setBalanceBefore(currentBalance);
        tx.setBalanceAfter(currentBalance);

        try {
            coinTransactionRepository.save(tx);
        } catch (DataIntegrityViolationException ex) {
            CoinTransaction existingTx = coinTransactionRepository
                    .findByUserIdAndIdempotencyKey(currentUser.getId(), idempotencyKey)
                    .orElseThrow(() -> ex);
            return toPurchaseResponse(existingTx, resolveCheckoutUrl(existingTx));
        }

        if (!isStripeMode()) {
            tx.setExternalPaymentId("mock-" + tx.getId());
            coinTransactionRepository.save(tx);
            return toPurchaseResponse(tx, null);
        }

        return createStripeCheckout(tx, selectedPack);
    }

    @Transactional
    public StreetCoinPurchaseConfirmResponse confirmPurchase(StreetCoinPurchaseConfirmRequest request) {
        RegularUser currentUser = requireCurrentRegularUser();
        String sessionId = requireSessionId(request.getSessionId());

        if (!isStripeMode()) {
            CoinTransaction tx = findMockTransaction(sessionId);
            return completeTransaction(currentUser.getId(), tx.getId(), sessionId);
        }

        ensureStripeConfigured();
        Stripe.apiKey = stripeSecretKey;

        try {
            Session session = Session.retrieve(sessionId);

            if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
                throw new AccessDeniedException("Payment has not been completed yet.");
            }

            Map<String, String> metadata = session.getMetadata();
            if (metadata == null || !"streetcoins".equalsIgnoreCase(metadata.get("flow"))) {
                throw new IllegalArgumentException("Stripe session does not belong to StreetCoins flow.");
            }

            UUID metadataUserId = parseUuid(metadata.get("userId"), "Stripe metadata userId is invalid.");
            if (!currentUser.getId().equals(metadataUserId)) {
                throw new AccessDeniedException("Stripe session does not belong to the authenticated user.");
            }

            UUID transactionId = parseUuid(metadata.get("transactionId"),
                    "Stripe metadata transactionId is invalid.");

            return completeTransaction(currentUser.getId(), transactionId, sessionId);
        } catch (StripeException ex) {
            throw new IllegalStateException("Unable to confirm StreetCoins checkout session.", ex);
        }
    }

    private StreetCoinPurchaseConfirmResponse completeTransaction(UUID userId, UUID transactionId, String sessionId) {
        CoinTransaction tx = coinTransactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("CoinTransaction", "id", transactionId));

        if (!tx.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Transaction does not belong to current user.");
        }

        if (tx.getType() != CoinTransactionType.PURCHASE) {
            throw new IllegalArgumentException("Only purchase transactions can be confirmed.");
        }

        if (tx.getStatus() == CoinTransactionStatus.SUCCESS) {
            int stableBalance = tx.getBalanceAfter() == null ? safeBalance(tx.getUser().getCoinBalance())
                    : tx.getBalanceAfter();
            return new StreetCoinPurchaseConfirmResponse(
                    tx.getId(),
                    "success",
                    tx.getAmount(),
                    stableBalance,
                    tx.getExternalPaymentId());
        }

        if (tx.getStatus() == CoinTransactionStatus.FAILED) {
            throw new IllegalStateException("This transaction is marked as failed.");
        }

        if (tx.getAmount() == null || tx.getAmount() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive.");
        }

        RegularUser lockedUser = regularUserRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("RegularUser", "id", userId));

        int balanceBefore = safeBalance(lockedUser.getCoinBalance());
        int balanceAfter = balanceBefore + tx.getAmount();

        lockedUser.setCoinBalance(balanceAfter);
        regularUserRepository.save(lockedUser);

        tx.setStatus(CoinTransactionStatus.SUCCESS);
        tx.setCurrency(STREET_COINS_CURRENCY);
        tx.setExternalPaymentId(sessionId);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        if (tx.getCreatedAt() == null) {
            tx.setCreatedAt(LocalDateTime.now());
        }

        coinTransactionRepository.save(tx);

        LOGGER.info("StreetCoins purchase confirmed for user={} transaction={} amount={} newBalance={}",
                userId, tx.getId(), tx.getAmount(), balanceAfter);

        return new StreetCoinPurchaseConfirmResponse(
                tx.getId(),
                "success",
                tx.getAmount(),
                balanceAfter,
                tx.getExternalPaymentId());
    }

    private StreetCoinPurchaseResponse createStripeCheckout(CoinTransaction tx, StreetCoinPack pack) {
        ensureStripeConfigured();
        Stripe.apiKey = stripeSecretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(appendQuery(streetCoinsSuccessUrl,
                        "payment=success&flow=streetcoins&session_id={CHECKOUT_SESSION_ID}"))
                .setCancelUrl(appendQuery(streetCoinsCancelUrl, "payment=cancel&flow=streetcoins"))
                .putMetadata("flow", "streetcoins")
                .putMetadata("userId", tx.getUser().getId().toString())
                .putMetadata("transactionId", tx.getId().toString())
                .putMetadata("packId", pack.getId())
                .putMetadata("streetCoins", String.valueOf(pack.getStreetCoins()))
                .putMetadata("idempotencyKey", tx.getIdempotencyKey())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(resolveStripeCurrency())
                                .setUnitAmount((long) pack.getAmountCents())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("StreetCoins Pack")
                                        .setDescription(
                                                pack.getLabel() + " - " + pack.getStreetCoins() + " StreetCoins")
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            Session session = Session.create(params);
            tx.setExternalPaymentId(session.getId());
            coinTransactionRepository.save(tx);

            LOGGER.info("StreetCoins checkout created: transaction={} sessionId={} user={}",
                    tx.getId(), session.getId(), tx.getUser().getId());

            return toPurchaseResponse(tx, session.getUrl());
        } catch (StripeException ex) {
            tx.setStatus(CoinTransactionStatus.FAILED);
            coinTransactionRepository.save(tx);
            throw new IllegalStateException("Unable to create Stripe checkout session for StreetCoins.", ex);
        }
    }

    private String resolveCheckoutUrl(CoinTransaction tx) {
        if (!isStripeMode() || !StringUtils.hasText(tx.getExternalPaymentId())) {
            return null;
        }

        ensureStripeConfigured();
        Stripe.apiKey = stripeSecretKey;

        try {
            Session session = Session.retrieve(tx.getExternalPaymentId());
            return session.getUrl();
        } catch (StripeException ex) {
            LOGGER.warn("Could not resolve checkout URL for session {}", tx.getExternalPaymentId(), ex);
            return null;
        }
    }

    private CoinTransaction findMockTransaction(String sessionId) {
        return coinTransactionRepository.findByExternalPaymentId(sessionId)
                .orElseGet(() -> {
                    if (sessionId.startsWith("mock-")) {
                        String possibleId = sessionId.substring("mock-".length());
                        if (StringUtils.hasText(possibleId)) {
                            try {
                                UUID transactionId = UUID.fromString(possibleId);
                                return coinTransactionRepository.findById(transactionId)
                                        .orElseThrow(() -> new ResourceNotFoundException("CoinTransaction",
                                                "externalPaymentId", sessionId));
                            } catch (IllegalArgumentException ignored) {
                                // Fallback to resource-not-found below.
                            }
                        }
                    }

                    throw new ResourceNotFoundException("CoinTransaction", "externalPaymentId", sessionId);
                });
    }

    private StreetCoinPurchaseResponse toPurchaseResponse(CoinTransaction tx, String checkoutUrl) {
        return new StreetCoinPurchaseResponse(
                tx.getId(),
                resolveStatus(tx),
                tx.getAmount(),
                resolveCurrency(tx),
                tx.getIdempotencyKey(),
                tx.getExternalPaymentId(),
                checkoutUrl,
                stripePublishableKey);
    }

    private String resolveStatus(CoinTransaction tx) {
        CoinTransactionStatus status = tx.getStatus();
        if (status == null) {
            return "success";
        }
        return status.name().toLowerCase(Locale.ROOT);
    }

    private String resolveCurrency(CoinTransaction tx) {
        return StringUtils.hasText(tx.getCurrency()) ? tx.getCurrency() : STREET_COINS_CURRENCY;
    }

    private String resolveDescription(CoinTransaction tx) {
        if (StringUtils.hasText(tx.getDescription())) {
            return tx.getDescription();
        }

        if (tx.getType() == CoinTransactionType.PURCHASE) {
            return "StreetCoins purchase";
        }
        if (tx.getType() == CoinTransactionType.EARN) {
            return "Coins earned";
        }
        if (tx.getType() == CoinTransactionType.SPEND) {
            return "Coins spent";
        }

        return "Transaction";
    }

    private String requireSessionId(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("Stripe sessionId is required.");
        }
        return sessionId.trim();
    }

    private String resolveIdempotencyKey(String headerValue, String bodyValue) {
        String key = StringUtils.hasText(headerValue) ? headerValue : bodyValue;
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Idempotency-Key is required.");
        }
        return key.trim();
    }

    private boolean isStripeMode() {
        return "stripe".equalsIgnoreCase(StringUtils.trimWhitespace(purchaseMode));
    }

    private void ensureStripeConfigured() {
        if (!StringUtils.hasText(stripeSecretKey)) {
            throw new IllegalStateException("Stripe secret key is not configured.");
        }
    }

    private String resolveStripeCurrency() {
        return StringUtils.hasText(stripeCurrency) ? stripeCurrency.trim().toLowerCase(Locale.ROOT) : "eur";
    }

    private String appendQuery(String baseUrl, String query) {
        String safeBaseUrl = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "http://localhost:8081";
        String separator = safeBaseUrl.contains("?") ? "&" : "?";
        return safeBaseUrl + separator + query;
    }

    private UUID parseUuid(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(errorMessage);
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(errorMessage, ex);
        }
    }

    private int safeBalance(Integer value) {
        return value == null ? 0 : value;
    }

    private RegularUser requireCurrentRegularUser() {
        User current = userService.findCurrentUser();
        if (!(current instanceof RegularUser regularUser)) {
            throw new AccessDeniedException("Only regular users can purchase StreetCoins.");
        }

        return regularUserRepository.findById(regularUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("RegularUser", "id", regularUser.getId()));
    }
}
