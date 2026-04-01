package com.streetask.app.user;

import java.time.LocalDateTime;
import java.util.List;

import com.streetask.app.model.Report;
import com.streetask.app.business.BusinessAccount;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admins")
@Getter
@Setter
public class Admin extends User {

    @Enumerated(EnumType.STRING)
    private AdminRole role;

    @Lob
    private String permissions;

    private LocalDateTime assignedAt;

    @OneToMany(mappedBy = "resolvedBy")
    private List<Report> resolvedReports;

    @OneToMany(mappedBy = "verifiedBy")
    private List<BusinessAccount> verifiedBusinessAccounts;
}
