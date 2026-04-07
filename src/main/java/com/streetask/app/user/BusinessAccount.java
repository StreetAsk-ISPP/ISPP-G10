package com.streetask.app.user;

import java.time.LocalDateTime;
import java.util.List;

import com.streetask.app.model.Event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "business_accounts")
@Getter
@Setter
public class BusinessAccount extends User {

    // Company legal/commercial name (not unique); the same company can operate in
    // multiple cities with different platform accounts (unique userName per account).
    @NotBlank
    @Column(nullable = false)
    private String companyName;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String taxId;

    private String address;

    private String website;

    private String description;

    private String logo;

    private Boolean verified;

    private Float rating;

    private LocalDateTime verifiedAt;

    @Enumerated(EnumType.STRING)
    private RequestStatus requestStatus;

    private Boolean subscriptionActive;

    private LocalDateTime subscriptionExpiresAt;

    @ManyToOne
    private Admin verifiedBy;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToMany(mappedBy = "creator")
    private List<Event> createdEvents;
}
