package com.streetask.app.business;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.streetask.app.model.Event;
import com.streetask.app.user.Admin;
import com.streetask.app.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "business_accounts")
@Getter
@Setter
public class BusinessAccount extends User {

    // Company legal/commercial name (not unique); the same company can operate in
    // multiple cities with different platform accounts (unique userName per
    // account).
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

    private Boolean verified = false;

    private Float rating = 0.0f;

    private Integer totalLikesReceived;

    private Integer totalDislikesReceived;

    private LocalDateTime verifiedAt;

    @Enumerated(EnumType.STRING)
    private RequestStatus requestStatus = RequestStatus.PENDING;

    private Boolean subscriptionActive = false;

    private LocalDateTime subscriptionExpiresAt;

    @ManyToOne
    private Admin verifiedBy;

    @JsonIgnore
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @JsonIgnore
    @OneToMany(mappedBy = "creator")
    private List<Event> createdEvents;

    @Transient
    public Boolean getPremiumActive() {
        return hasEffectivePremiumAccess();
    }

    public boolean hasEffectivePremiumAccess() {
        return Boolean.TRUE.equals(verified)
                && Boolean.TRUE.equals(subscriptionActive)
                && subscriptionExpiresAt != null
                && subscriptionExpiresAt.isAfter(LocalDateTime.now());
    }
}
