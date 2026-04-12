package com.streetask.app.auth.payload.request;

import com.streetask.app.user.AccountType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRoleRequest {

    @NotNull(message = "New account type is required")
    private AccountType newAccountType;

    private String reason; // Optional: reason for change (for audit log)
}
