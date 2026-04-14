package com.streetask.app.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.streetask.app.auth.AuthService;
import com.streetask.app.auth.payload.request.BusinessSignupRequest;
import com.streetask.app.exceptions.AccessDeniedException;
import com.streetask.app.exceptions.ResourceNotFoundException;
import com.streetask.app.user.AccountType;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.User;
import com.streetask.app.user.UserService;

@ExtendWith(MockitoExtension.class)
class BusinessSubscriptionServiceTest {

    @Mock
    private BusinessAccountRepository businessAccountRepository;

    @Mock
    private BusinessPremiumAccessGuard businessPremiumAccessGuard;

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    private BusinessSubscriptionService service;

    private BusinessAccount businessAccount;

    @BeforeEach
    void setUp() {
        service = new BusinessSubscriptionService(businessAccountRepository, businessPremiumAccessGuard, authService,
                userService);
        ReflectionTestUtils.setField(service, "stripeSecretKey", "test-secret");
        ReflectionTestUtils.setField(service, "stripePublishableKey", "test-publishable");
        ReflectionTestUtils.setField(service, "stripeCurrency", "eur");
        ReflectionTestUtils.setField(service, "stripeSubscriptionAmountCents", 1999);
        ReflectionTestUtils.setField(service, "stripeSuccessUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(service, "stripeCancelUrl", "http://localhost:8081");

        businessAccount = new BusinessAccount();
        businessAccount.setId(UUID.randomUUID());
        businessAccount.setEmail("biz@streetask.com");
        businessAccount.setCompanyName("Biz Co");
        businessAccount.setVerified(true);
        businessAccount.setSubscriptionActive(false);
        businessAccount.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(1));
    }

    @Test
    void getCurrentBusinessStatusShouldReturnResponseForBusinessUser() {
        when(userService.findCurrentUser()).thenReturn(businessAccount);
        when(businessPremiumAccessGuard.hasPremiumAccess(businessAccount)).thenReturn(false);

        BusinessSubscriptionStatusResponse response = service.getCurrentBusinessStatus();

        assertThat(response.getBusinessId()).isEqualTo(businessAccount.getId());
        assertThat(response.getEmail()).isEqualTo("biz@streetask.com");
        verify(userService).findCurrentUser();
    }

    @Test
    void activateCurrentBusinessMockSubscriptionShouldUseDefaultDurationAndPersistAccount() {
        when(userService.findCurrentUser()).thenReturn(businessAccount);
        when(businessPremiumAccessGuard.hasPremiumAccess(businessAccount)).thenReturn(false);

        BusinessSubscriptionStatusResponse response = service.activateCurrentBusinessMockSubscription(null);

        assertThat(response.getSubscriptionActive()).isTrue();
        verify(businessPremiumAccessGuard).requireVerified(businessAccount);
        verify(businessAccountRepository).save(businessAccount);
    }

    @Test
    void activateMockSubscriptionShouldRejectWhenBusinessNotFound() {
        when(businessAccountRepository.findByEmailAndTaxId("biz@streetask.com", "B12345678"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateMockSubscription(" biz@streetask.com ", "b-1234 5678", 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activateMockSubscriptionShouldActivateAndPersistWhenBusinessExists() {
        BusinessAccount existing = new BusinessAccount();
        existing.setId(UUID.randomUUID());
        existing.setEmail("biz@streetask.com");
        existing.setCompanyName("Biz Co");
        existing.setVerified(true);

        when(businessAccountRepository.findByEmailAndTaxId("biz@streetask.com", "B12345678"))
                .thenReturn(Optional.of(existing));

        BusinessSubscriptionStatusResponse response = service.activateMockSubscription(
                " biz@streetask.com ", "b-1234 5678", 15);

        assertThat(response.getSubscriptionActive()).isTrue();
        verify(businessPremiumAccessGuard).requireVerified(existing);
        verify(businessAccountRepository).save(existing);
    }

    @Test
    void requireCurrentBusinessPremiumAccessShouldDelegateToGuard() {
        when(userService.findCurrentUser()).thenReturn(businessAccount);

        service.requireCurrentBusinessPremiumAccess();

        verify(businessPremiumAccessGuard).requirePremiumAccess(businessAccount);
    }

    @Test
    void createPublicStripeCheckoutSessionShouldRejectWhenPendingSignupIsMissing() {
        when(userService.findUser("missing@streetask.com")).thenReturn(null);

        assertThatThrownBy(() -> service.createPublicStripeCheckoutSession(
                "missing@streetask.com", "B12345678", "Biz", "Addr", "site", "desc", 10))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Basic user registration not found. Please complete the basic signup first.");
    }

    @Test
    void createPublicStripeCheckoutSessionShouldRejectWhenTaxIdAlreadyRegistered() {
        User pending = new User();
        pending.setAccountType(null);
        pending.setActive(false);
        pending.setAuthority(new com.streetask.app.user.Authorities());
        pending.getAuthority().setAuthority("USER");
        when(userService.findUser("pending@streetask.com")).thenReturn(pending);
        when(businessAccountRepository.existsByTaxId("B12345678")).thenReturn(true);

        assertThatThrownBy(() -> service.createPublicStripeCheckoutSession(
                "pending@streetask.com", "B12345678", "Biz", "Addr", "site", "desc", 10))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Tax ID is already registered.");
    }

    @Test
    void createPublicStripeCheckoutSessionShouldReachPublicSignupHelperBeforeStripeConfigurationCheck() {
        User pending = new User();
        pending.setAccountType(null);
        pending.setActive(false);
        pending.setAuthority(new com.streetask.app.user.Authorities());
        pending.getAuthority().setAuthority("USER");
        when(userService.findUser("pending@streetask.com")).thenReturn(pending);
        when(businessAccountRepository.existsByTaxId("B12345678")).thenReturn(false);
        ReflectionTestUtils.setField(service, "stripeSecretKey", "");

        assertThatThrownBy(() -> service.createPublicStripeCheckoutSession(
                "pending@streetask.com", "B12345678", "Biz", null, null, null, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stripe secret key is not configured.");
    }

    @Test
    void createCurrentBusinessStripeCheckoutSessionShouldRejectWhenPremiumAlreadyActive() {
        when(userService.findCurrentUser()).thenReturn(businessAccount);
        when(businessPremiumAccessGuard.hasPremiumAccess(businessAccount)).thenReturn(true);

        assertThatThrownBy(() -> service.createCurrentBusinessStripeCheckoutSession(10))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Business premium access is already active.");
    }

    @Test
    void createCurrentBusinessStripeCheckoutSessionShouldFailFastWhenStripeIsNotConfigured() {
        when(userService.findCurrentUser()).thenReturn(businessAccount);
        ReflectionTestUtils.setField(service, "stripeSecretKey", "");

        assertThatThrownBy(() -> service.createCurrentBusinessStripeCheckoutSession(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stripe secret key is not configured.");
    }

    @Test
    void confirmPublicStripeCheckoutSessionShouldRejectWhenSessionIdIsBlank() {
        PublicStripeCheckoutSessionConfirmRequest request = new PublicStripeCheckoutSessionConfirmRequest();
        request.setEmail("biz@streetask.com");
        request.setTaxId("B12345678");
        request.setSessionId(" ");

        assertThatThrownBy(() -> service.confirmPublicStripeCheckoutSession(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stripe sessionId is required.");
    }

    @Test
    void normalizeHelpersAndStatusResponseShouldBeCovered() {
        assertThat((String) ReflectionTestUtils.invokeMethod(service, "normalizeEmail", "  Biz@StreetAsk.Com  "))
                .isEqualTo("biz@streetask.com");
        assertThat((String) ReflectionTestUtils.invokeMethod(service, "normalizeEmail", (String) null)).isEqualTo("");

        assertThat((String) ReflectionTestUtils.invokeMethod(service, "normalizeTaxId", " b-12 34 "))
                .isEqualTo("B1234");
        assertThat((String) ReflectionTestUtils.invokeMethod(service, "normalizeTaxId", (String) null)).isEqualTo("");

        assertThat((Integer) ReflectionTestUtils.invokeMethod(service, "resolveDurationDays", (Integer) null))
                .isEqualTo(30);
        assertThat((Integer) ReflectionTestUtils.invokeMethod(service, "resolveDurationDays", 0)).isEqualTo(30);
        assertThat((Integer) ReflectionTestUtils.invokeMethod(service, "resolveDurationDays", 12)).isEqualTo(12);

        assertThat((String) ReflectionTestUtils.invokeMethod(service, "normalizeCurrency")).isEqualTo("eur");
        assertThat((Long) ReflectionTestUtils.invokeMethod(service, "resolveAmount")).isEqualTo(1999L);

        assertThat((String) ReflectionTestUtils.invokeMethod(service, "appendQuery", "http://localhost:8081", "a=b"))
                .isEqualTo("http://localhost:8081?a=b");
        assertThat(
                (String) ReflectionTestUtils.invokeMethod(service, "appendQuery", "http://localhost:8081?x=1", "a=b"))
                .isEqualTo("http://localhost:8081?x=1&a=b");

        BusinessAccount statusAccount = new BusinessAccount();
        statusAccount.setId(UUID.randomUUID());
        statusAccount.setEmail("status@streetask.com");
        statusAccount.setCompanyName("Status Co");
        statusAccount.setVerified(true);
        statusAccount.setRequestStatus(RequestStatus.APPROVED);
        statusAccount.setSubscriptionActive(true);
        statusAccount.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(2));
        when(businessPremiumAccessGuard.hasPremiumAccess(statusAccount)).thenReturn(true);

        BusinessSubscriptionStatusResponse response = ReflectionTestUtils.invokeMethod(service, "toStatusResponse",
                statusAccount);

        assertThat(response.getBusinessId()).isEqualTo(statusAccount.getId());
        assertThat(response.getPremiumEligible()).isTrue();
    }

    @Test
    void findBusinessByEmailAndTaxIdShouldReturnBusinessWhenPresent() {
        BusinessAccount stored = new BusinessAccount();
        stored.setEmail("biz@streetask.com");
        stored.setTaxId("B12345678");

        when(businessAccountRepository.findByEmailAndTaxId("biz@streetask.com", "B12345678"))
                .thenReturn(Optional.of(stored));

        BusinessAccount result = ReflectionTestUtils.invokeMethod(service, "findBusinessByEmailAndTaxId",
                " biz@streetask.com ", "b-1234 5678");

        assertThat(result).isEqualTo(stored);
    }

    @Test
    void getCurrentBusinessStatusShouldRejectWhenCurrentUserIsNotBusiness() {
        User regularUser = new User();
        when(userService.findCurrentUser()).thenReturn(regularUser);

        assertThatThrownBy(() -> service.getCurrentBusinessStatus())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only business accounts can access this endpoint.");
    }

    @Test
    void requireCurrentBusinessPremiumAccessShouldRejectWhenGuardDoes() {
        when(userService.findCurrentUser()).thenReturn(businessAccount);
        org.mockito.Mockito.doThrow(new AccessDeniedException("Business subscription is not active."))
                .when(businessPremiumAccessGuard).requirePremiumAccess(businessAccount);

        assertThatThrownBy(() -> service.requireCurrentBusinessPremiumAccess())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Business subscription is not active.");
    }

    @Test
    void realGuardShouldCoverVerifiedAndPremiumBranches() {
        BusinessPremiumAccessGuard realGuard = new BusinessPremiumAccessGuard();

        BusinessAccount unverified = new BusinessAccount();
        unverified.setVerified(false);
        assertThatThrownBy(() -> realGuard.requireVerified(unverified))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Business account must be verified.");

        BusinessAccount inactive = new BusinessAccount();
        inactive.setVerified(true);
        inactive.setSubscriptionActive(false);
        inactive.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(1));
        assertThatThrownBy(() -> realGuard.requirePremiumAccess(inactive))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Business subscription is not active.");

        BusinessAccount expired = new BusinessAccount();
        expired.setVerified(true);
        expired.setSubscriptionActive(true);
        expired.setSubscriptionExpiresAt(LocalDateTime.now().minusDays(1));
        assertThatThrownBy(() -> realGuard.requirePremiumAccess(expired))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Business subscription is expired.");

        BusinessAccount premium = new BusinessAccount();
        premium.setVerified(true);
        premium.setSubscriptionActive(true);
        premium.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(1));
        assertThat(realGuard.hasPremiumAccess(premium)).isTrue();
    }

    @Test
    void toStatusResponseShouldExposePremiumEligibility() throws Exception {
        businessAccount.setSubscriptionActive(true);
        businessAccount.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(1));

        Field method = BusinessSubscriptionService.class.getDeclaredField("businessPremiumAccessGuard");
        method.setAccessible(true);
        ReflectionTestUtils.setField(service, "businessPremiumAccessGuard", businessPremiumAccessGuard);
        when(businessPremiumAccessGuard.hasPremiumAccess(businessAccount)).thenReturn(true);

        BusinessSubscriptionStatusResponse response = ReflectionTestUtils.invokeMethod(service, "toStatusResponse",
                businessAccount);

        assertThat(response.getPremiumEligible()).isTrue();
    }
}