package com.streetask.app.moderation;

import java.time.LocalDateTime;
import java.util.UUID;

import com.streetask.app.user.BusinessAccount;
import com.streetask.app.user.RequestStatus;

import lombok.Getter;

@Getter
public class BusinessVerificationDTO {

    private final UUID id;
    private final String companyName;
    private final String taxId;
    private final String address;
    private final String website;
    private final String description;
    private final String logo;
    private final Boolean verified;
    private final RequestStatus requestStatus;
    private final LocalDateTime verifiedAt;
    private final String rejectionReason;
    private final String ownerEmail;
    private final String ownerUserName;
    private final String ownerFirstName;
    private final String ownerLastName;
    private final LocalDateTime createdAt;

    public BusinessVerificationDTO(BusinessAccount account) {
        this.id = account.getId();
        this.companyName = account.getCompanyName();
        this.taxId = account.getTaxId();
        this.address = account.getAddress();
        this.website = account.getWebsite();
        this.description = account.getDescription();
        this.logo = account.getLogo();
        this.verified = account.getVerified();
        this.requestStatus = account.getRequestStatus();
        this.verifiedAt = account.getVerifiedAt();
        this.rejectionReason = account.getRejectionReason();
        this.ownerEmail = account.getEmail();
        this.ownerUserName = account.getUserName();
        this.ownerFirstName = account.getFirstName();
        this.ownerLastName = account.getLastName();
        this.createdAt = account.getCreatedAt();
    }
}
