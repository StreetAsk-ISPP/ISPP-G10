package com.streetask.app.business;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streetask.app.auth.payload.response.MessageResponse;
import com.streetask.app.payments.CheckoutReturnUrlRequestResolver;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/business-subscriptions")
@SecurityRequirement(name = "bearerAuth")
public class BusinessSubscriptionRestController {

    private final BusinessSubscriptionService businessSubscriptionService;
    private final CheckoutReturnUrlRequestResolver checkoutReturnUrlRequestResolver;

    public BusinessSubscriptionRestController(
            BusinessSubscriptionService businessSubscriptionService,
            CheckoutReturnUrlRequestResolver checkoutReturnUrlRequestResolver) {
        this.businessSubscriptionService = businessSubscriptionService;
        this.checkoutReturnUrlRequestResolver = checkoutReturnUrlRequestResolver;
    }

    @PostMapping("/mock/activate")
    public ResponseEntity<BusinessSubscriptionStatusResponse> activateMockSubscription(
            @Valid @RequestBody MockBusinessSubscriptionActivationRequest request) {
        BusinessSubscriptionStatusResponse response = businessSubscriptionService.activateMockSubscription(
                request.getEmail(), request.getTaxId(), request.getDurationDays());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/stripe/checkout-session")
    public ResponseEntity<StripeCheckoutSessionResponse> createStripeCheckoutSession(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        String email = request.get("email") == null ? null : request.get("email").toString();
        String taxId = request.get("taxId") == null ? null : request.get("taxId").toString();
        String companyName = request.get("companyName") == null ? null : request.get("companyName").toString();
        String address = request.get("address") == null ? null : request.get("address").toString();
        String website = request.get("website") == null ? null : request.get("website").toString();
        String description = request.get("description") == null ? null : request.get("description").toString();
        Integer durationDays = request.get("durationDays") == null ? null
            : Integer.valueOf(request.get("durationDays").toString());
        String requestedReturnUrl = checkoutReturnUrlRequestResolver.resolve(httpRequest);

        StripeCheckoutSessionResponse response = businessSubscriptionService.createPublicStripeCheckoutSession(
            email, taxId, companyName, address, website, description, durationDays, requestedReturnUrl);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/stripe/confirm-session")
    public ResponseEntity<BusinessSubscriptionStatusResponse> confirmStripeCheckoutSession(
            @Valid @RequestBody PublicStripeCheckoutSessionConfirmRequest request) {
        BusinessSubscriptionStatusResponse response = businessSubscriptionService.confirmPublicStripeCheckoutSession(
                request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<BusinessSubscriptionStatusResponse> getCurrentBusinessSubscriptionStatus() {
        return new ResponseEntity<>(businessSubscriptionService.getCurrentBusinessStatus(), HttpStatus.OK);
    }

    @PostMapping("/me/mock/activate")
    public ResponseEntity<BusinessSubscriptionStatusResponse> activateCurrentBusinessMockSubscription(
            @RequestBody(required = false) MockBusinessSubscriptionActivationRequest request) {
        Integer durationDays = request == null ? null : request.getDurationDays();
        return new ResponseEntity<>(
                businessSubscriptionService.activateCurrentBusinessMockSubscription(durationDays),
                HttpStatus.OK);
    }

    @PostMapping("/me/stripe/checkout-session")
    public ResponseEntity<StripeCheckoutSessionResponse> createCurrentBusinessStripeCheckoutSession(
            @RequestBody(required = false) MockBusinessSubscriptionActivationRequest request,
            HttpServletRequest httpRequest) {
        Integer durationDays = request == null ? null : request.getDurationDays();
        String requestedReturnUrl = checkoutReturnUrlRequestResolver.resolve(httpRequest);
        StripeCheckoutSessionResponse response = businessSubscriptionService
                .createCurrentBusinessStripeCheckoutSession(durationDays, requestedReturnUrl);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/me/stripe/confirm-session")
    public ResponseEntity<BusinessSubscriptionStatusResponse> confirmCurrentBusinessStripeCheckoutSession(
            @Valid @RequestBody StripeCheckoutSessionConfirmRequest request) {
        BusinessSubscriptionStatusResponse response = businessSubscriptionService
                .confirmCurrentBusinessStripeCheckoutSession(request.getSessionId());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/premium/access-check")
    public ResponseEntity<MessageResponse> checkPremiumAccess() {
        businessSubscriptionService.requireCurrentBusinessPremiumAccess();
        return new ResponseEntity<>(new MessageResponse("Premium access granted."), HttpStatus.OK);
    }
}
