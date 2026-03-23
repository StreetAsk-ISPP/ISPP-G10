package com.streetask.app.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streetask.app.auth.payload.response.MessageResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/business-subscriptions")
@SecurityRequirement(name = "bearerAuth")
public class BusinessSubscriptionRestController {

    private final BusinessSubscriptionService businessSubscriptionService;

    public BusinessSubscriptionRestController(BusinessSubscriptionService businessSubscriptionService) {
        this.businessSubscriptionService = businessSubscriptionService;
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
            @Valid @RequestBody MockBusinessSubscriptionActivationRequest request) {
        StripeCheckoutSessionResponse response = businessSubscriptionService.createPublicStripeCheckoutSession(
                request.getEmail(), request.getTaxId(), request.getDurationDays());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/stripe/confirm-session")
    public ResponseEntity<BusinessSubscriptionStatusResponse> confirmStripeCheckoutSession(
            @Valid @RequestBody PublicStripeCheckoutSessionConfirmRequest request) {
        BusinessSubscriptionStatusResponse response = businessSubscriptionService.confirmPublicStripeCheckoutSession(
                request.getEmail(), request.getTaxId(), request.getSessionId());
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
            @RequestBody(required = false) MockBusinessSubscriptionActivationRequest request) {
        Integer durationDays = request == null ? null : request.getDurationDays();
        StripeCheckoutSessionResponse response = businessSubscriptionService
                .createCurrentBusinessStripeCheckoutSession(durationDays);
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
