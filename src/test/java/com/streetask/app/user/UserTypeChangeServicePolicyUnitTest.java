package com.streetask.app.user;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.streetask.app.exceptions.AccessDeniedException;

class UserTypeChangeServicePolicyUnitTest {

    @Test
    void regularUserCannotUpgradeToBusinessInSelfServiceFlow() {
        UserTypeChangeService service = new UserTypeChangeService(
                null,
                null,
                null,
                null,
                null,
                null);

        RegularUser regularUser = new RegularUser();
        regularUser.setAccountType(AccountType.REGULAR_USER);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> service.changeAccountType(regularUser, AccountType.BUSINESS, null, "Self upgrade", null));

        assertTrue(exception.getMessage().contains("cannot change their account type to Business"));
    }
}
