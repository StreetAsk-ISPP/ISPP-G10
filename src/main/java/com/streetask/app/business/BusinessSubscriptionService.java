package com.streetask.app.business;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.user.User;
import com.streetask.app.user.UserService;

@Service
public class BusinessSubscriptionService {

    private static final int DEFAULT_DURATION_DAYS = 30;

    private final BusinessAccountRepository businessAccountRepository;
    private final BusinessPremiumAccessGuard businessPremiumAccessGuard;
    private final UserService userService;

    @Value("${streetask.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${streetask.stripe.publishable-key:}")
    private String stripePublishableKey;

    @Value("${streetask.stripe.currency:eur}")
    private String stripeCurrency;

    @Value("${streetask.stripe.subscription-amount-cents:1999}")
    private Integer stripeSubscriptionAmountCents;

    @Value("${streetask.stripe.success-url:http://localhost:8081}")
    private String stripeSuccessUrl;

    @Value("${streetask.stripe.cancel-url:http://localhost:8081}")
    private String stripeCancelUrl;

    public BusinessSubscriptionService(BusinessAccountRepository businessAccountRepository,
            BusinessPremiumAccessGuard businessPremiumAccessGuard, UserService userService) {
        this.businessAccountRepository = businessAccountRepository;
        this.businessPremiumAccessGuard = businessPremiumAccessGuard;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public BusinessSubscriptionStatusResponse getCurrentBusinessStatus() {
        BusinessAccount businessAccount = getCurrentBusinessAccount();
        return toStatusResponse(businessAccount);
    }

    @Transactional
    public BusinessSubscriptionStatusResponse activateCurrentBusinessMockSubscription(Integer durationDays) {
        BusinessAccount businessAccount = getCurrentBusinessAccount();
        return activateSubscription(businessAccount, durationDays);
    }

    @Transactional
    public BusinessSubscriptionStatusResponse activateMockSubscription(String email, String taxId,
            Integer durationDays) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedTaxId = normalizeTaxId(taxId);

        BusinessAccount businessAccount = businessAccountRepository
                .findByEmailAndTaxId(normalizedEmail, normalizedTaxId)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessAccount", "email/taxId",
                        normalizedEmail + "/" + normalizedTaxId));

        return activateSubscription(businessAccount, durationDays);
    }

    @Transactional(readOnly = true)
    public void requireCurrentBusinessPremiumAccess() {
        BusinessAccount businessAccount = getCurrentBusinessAccount();
        businessPremiumAccessGuard.requirePremiumAccess(businessAccount);
    }

    @Transactional(readOnly = true)
    public StripeCheckoutSessionResponse createCurrentBusinessStripeCheckoutSession(Integer durationDays) {
        BusinessAccount businessAccount = getCurrentBusinessAccount();
        return createStripeCheckoutSession(businessAccount, durationDays);
    }

    @Transactional(readOnly = true)
    public StripeCheckoutSessionResponse createPublicStripeCheckoutSession(String email, String taxId,
            Integer durationDays) {
        BusinessAccount businessAccount = findBusinessByEmailAndTaxId(email, taxId);
        return createStripeCheckoutSession(businessAccount, durationDays);
    }

    @Transactional
    public BusinessSubscriptionStatusResponse confirmCurrentBusinessStripeCheckoutSession(String sessionId) {
        BusinessAccount businessAccount = getCurrentBusinessAccount();
        return confirmStripeCheckoutSession(businessAccount, sessionId);
    }

    @Transactional
    public BusinessSubscriptionStatusResponse confirmPublicStripeCheckoutSession(String email, String taxId,
            String sessionId) {
        BusinessAccount businessAccount = findBusinessByEmailAndTaxId(email, taxId);
        return confirmStripeCheckoutSession(businessAccount, sessionId);
    }

    private BusinessSubscriptionStatusResponse activateSubscription(BusinessAccount businessAccount,
            Integer durationDays) {
        businessPremiumAccessGuard.requireVerified(businessAccount);

        int resolvedDurationDays = resolveDurationDays(durationDays);
        LocalDateTime now = LocalDateTime.now();

        businessAccount.setSubscriptionActive(true);
        businessAccount.setSubscriptionExpiresAt(now.plusDays(resolvedDurationDays));
        businessAccountRepository.save(businessAccount);

        return toStatusResponse(businessAccount);
    }

    private BusinessAccount getCurrentBusinessAccount() {
        User currentUser = userService.findCurrentUser();
        if (!(currentUser instanceof BusinessAccount businessAccount)) {
            throw new AccessDeniedException("Only business accounts can access this endpoint.");
        }
        return businessAccount;
    }

    private int resolveDurationDays(Integer durationDays) {
        if (durationDays == null || durationDays <= 0) {
            return DEFAULT_DURATION_DAYS;
        }

        return durationDays;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeTaxId(String taxId) {
        return taxId == null ? "" : taxId.trim().toUpperCase(Locale.ROOT).replace(" ", "").replace("-", "");
    }

    private StripeCheckoutSessionResponse createStripeCheckoutSession(BusinessAccount businessAccount,
            Integer durationDays) {
        ensureStripeConfigured();

        if (businessPremiumAccessGuard.hasPremiumAccess(businessAccount)) {
            throw new AccessDeniedException("Business premium access is already active.");
        }

        Stripe.apiKey = stripeSecretKey;
        int resolvedDurationDays = resolveDurationDays(durationDays);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(appendQuery(stripeSuccessUrl, "payment=success&session_id={CHECKOUT_SESSION_ID}"))
                .setCancelUrl(appendQuery(stripeCancelUrl, "payment=cancel"))
                .putMetadata("businessId", businessAccount.getId().toString())
                .putMetadata("durationDays", String.valueOf(resolvedDurationDays))
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(normalizeCurrency())
                                                .setUnitAmount(resolveAmount())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("StreetAsk Business Subscription")
                                                                .setDescription("Business premium features activation")
                                                                .build())
                                                .build())
                                .build())
                .build();

        try {
            Session session = Session.create(params);
            return new StripeCheckoutSessionResponse(session.getId(), session.getUrl(), stripePublishableKey);
        } catch (StripeException ex) {
            throw new IllegalStateException("Unable to create Stripe checkout session.", ex);
        }
    }

    private BusinessSubscriptionStatusResponse confirmStripeCheckoutSession(BusinessAccount businessAccount,
            String sessionId) {
        ensureStripeConfigured();

        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("Stripe sessionId is required.");
        }

        Stripe.apiKey = stripeSecretKey;

        try {
            Session session = Session.retrieve(sessionId.trim());

            if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
                throw new AccessDeniedException("Payment has not been completed yet.");
            }

            String metadataBusinessId = session.getMetadata() == null ? null : session.getMetadata().get("businessId");
            if (!businessAccount.getId().toString().equals(metadataBusinessId)) {
                throw new AccessDeniedException("Stripe session does not belong to this business account.");
            }

            String durationDaysMetadata = session.getMetadata() == null ? null
                    : session.getMetadata().get("durationDays");
            Integer durationDays = durationDaysMetadata == null ? null : Integer.valueOf(durationDaysMetadata);

            LocalDateTime now = LocalDateTime.now();
            businessAccount.setSubscriptionActive(true);
            businessAccount.setSubscriptionExpiresAt(now.plusDays(resolveDurationDays(durationDays)));
            businessAccountRepository.save(businessAccount);

            return toStatusResponse(businessAccount);
        } catch (StripeException ex) {
            throw new IllegalStateException("Unable to confirm Stripe checkout session.", ex);
        }
    }

    private BusinessAccount findBusinessByEmailAndTaxId(String email, String taxId) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedTaxId = normalizeTaxId(taxId);

        return businessAccountRepository.findByEmailAndTaxId(normalizedEmail, normalizedTaxId)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessAccount", "email/taxId",
                        normalizedEmail + "/" + normalizedTaxId));
    }

    private void ensureStripeConfigured() {
        if (!StringUtils.hasText(stripeSecretKey)) {
            throw new IllegalStateException("Stripe secret key is not configured.");
        }
    }

    private String normalizeCurrency() {
        return StringUtils.hasText(stripeCurrency) ? stripeCurrency.trim().toLowerCase(Locale.ROOT) : "eur";
    }

    private Long resolveAmount() {
        int amount = stripeSubscriptionAmountCents == null ? 1999 : stripeSubscriptionAmountCents;
        return (long) Math.max(amount, 1);
    }

    private String appendQuery(String baseUrl, String query) {
        String safeBaseUrl = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "http://localhost:8081";
        String separator = safeBaseUrl.contains("?") ? "&" : "?";
        return safeBaseUrl + separator + query;
    }

    private BusinessSubscriptionStatusResponse toStatusResponse(BusinessAccount businessAccount) {
        return new BusinessSubscriptionStatusResponse(
                businessAccount.getId(),
                businessAccount.getEmail(),
                businessAccount.getCompanyName(),
                Boolean.TRUE.equals(businessAccount.getVerified()),
                Boolean.TRUE.equals(businessAccount.getSubscriptionActive()),
                businessAccount.getSubscriptionExpiresAt(),
                businessPremiumAccessGuard.hasPremiumAccess(businessAccount));
    }
}
