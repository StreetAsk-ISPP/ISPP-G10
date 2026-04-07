package com.streetask.app.moderation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.streetask.app.user.RequestStatus;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/moderation/businesses")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class BusinessVerificationController {

    private final BusinessVerificationService businessVerificationService;

    @GetMapping
    public ResponseEntity<List<BusinessVerificationDTO>> listByStatus(
            @RequestParam(defaultValue = "PENDING") RequestStatus status) {
        return ResponseEntity.ok(businessVerificationService.listByStatus(status));
    }

    @PostMapping("/{businessId}/approve")
    public ResponseEntity<BusinessVerificationDTO> approve(@PathVariable UUID businessId) {
        return ResponseEntity.ok(businessVerificationService.approve(businessId));
    }

    @PostMapping("/{businessId}/reject")
    public ResponseEntity<BusinessVerificationDTO> reject(
            @PathVariable UUID businessId,
            @RequestBody(required = false) VerificationActionRequest body) {
        String reason = body != null ? body.getReason() : null;
        return ResponseEntity.ok(businessVerificationService.reject(businessId, reason));
    }
}
