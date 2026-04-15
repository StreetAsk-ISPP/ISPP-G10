package com.streetask.app.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streetask.app.moderation.BusinessVerificationDTO;
import com.streetask.app.moderation.BusinessVerificationService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/businesses")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class BusinessStatusController {

    private final BusinessVerificationService businessVerificationService;

    @GetMapping("/me/verification")
    public ResponseEntity<BusinessVerificationDTO> getMyVerificationStatus() {
        return ResponseEntity.ok(businessVerificationService.getMyVerificationStatus());
    }
}
