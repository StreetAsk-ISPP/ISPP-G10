package com.streetask.app.user;

import java.time.LocalDateTime;
import java.util.UUID;

import com.streetask.app.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_role_change_logs")
public class UserRoleChangeLog extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String previousAccountType;

    @Column(nullable = false, length = 50)
    private String newAccountType;

    @Column(nullable = false, length = 50)
    private String previousAuthority;

    @Column(nullable = false, length = 50)
    private String newAuthority;

    @Column(length = 255)
    private String changedBy; // ADMIN user ID who made the change, or "SELF" if user triggered it

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 500)
    private String reason; // Optional reason for the change

    @Column(length = 500)
    private String ipAddress; // Track origin

    public UserRoleChangeLog() {
    }

    public UserRoleChangeLog(UUID userId, String previousAccountType, String newAccountType,
            String previousAuthority, String newAuthority, String changedBy,
            String reason, String ipAddress) {
        this.userId = userId;
        this.previousAccountType = previousAccountType;
        this.newAccountType = newAccountType;
        this.previousAuthority = previousAuthority;
        this.newAuthority = newAuthority;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
        this.reason = reason;
        this.ipAddress = ipAddress;
    }
}
