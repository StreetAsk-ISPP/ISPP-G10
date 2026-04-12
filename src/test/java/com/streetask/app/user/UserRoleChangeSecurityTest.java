package com.streetask.app.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.streetask.app.business.BusinessAccount;
import com.streetask.app.business.BusinessAccountRepository;
import com.streetask.app.exceptions.AccessDeniedException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserRoleChangeSecurityTest {

    @Autowired
    private UserTypeChangeService userTypeChangeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegularUserRepository regularUserRepository;

    @Autowired
    private BusinessAccountRepository businessAccountRepository;

    @Autowired
    private AuthoritiesService authoritiesService;

    private Admin adminUser;
    private RegularUser regularUser;
    private BusinessAccount businessUser;

    @BeforeEach
    public void setUp() {
        // Create admin user
        adminUser = new Admin();
        adminUser.setEmail("admin@test.com");
        adminUser.setUserName("admin_user");
        adminUser.setPassword("encoded");
        adminUser.setFirstName("A");
        adminUser.setLastName("D");
        adminUser.setAccountType(AccountType.ADMIN);
        adminUser.setActive(true);
        adminUser.setAuthority(authoritiesService.findByAuthority("ADMIN"));

        // Create regular user
        regularUser = new RegularUser();
        regularUser.setEmail("regular@test.com");
        regularUser.setUserName("regular_user");
        regularUser.setPassword("encoded");
        regularUser.setFirstName("R");
        regularUser.setLastName("User");
        regularUser.setAccountType(AccountType.REGULAR_USER);
        regularUser.setActive(true);
        regularUser.setAuthority(authoritiesService.findByAuthority("USER"));
        regularUser.setCoinBalance(0);
        regularUser.setVerified(false);

        // Create business user
        businessUser = new BusinessAccount();
        businessUser.setEmail("business@test.com");
        businessUser.setUserName("business_user");
        businessUser.setPassword("encoded");
        businessUser.setFirstName("B");
        businessUser.setLastName("User");
        businessUser.setAccountType(AccountType.BUSINESS);
        businessUser.setActive(true);
        businessUser.setAuthority(authoritiesService.findByAuthority("BUSINESS"));
        businessUser.setCompanyName("Test Corp");
        businessUser.setTaxId("12345678X");
        businessUser.setVerified(false);

        userRepository.save(adminUser);
        regularUserRepository.save(regularUser);
        businessAccountRepository.save(businessUser);
    }

    // ========== SCENARIO 1: ADMIN Cannot Downgrade From ADMIN ==========
    @Test
    @DisplayName("SCENARIO 1: Admin cannot change their own role to BUSINESS")
    public void testAdminCannotDowngradeFromAdmin() {
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            userTypeChangeService.changeAccountType(adminUser, AccountType.BUSINESS, null, "Test", null);
        });
        assertEquals("Admin users cannot change their account type.", exception.getMessage());
    }

    // ========== SCENARIO 2: REGULAR User Cannot Upgrade to BUSINESS ==========
    @Test
    @DisplayName("SCENARIO 2: Regular user CANNOT upgrade to BUSINESS (self)")
    public void testRegularUserCannotUpgradeToBusiness() {
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            userTypeChangeService.changeAccountType(regularUser, AccountType.BUSINESS, null, "Self upgrade", null);
        });
        assertTrue(exception.getMessage().contains("cannot change their account type to Business"));
    }

    // ========== SCENARIO 3: REGULAR User Cannot Downgrade via Self ==========
    @Test
    @DisplayName("SCENARIO 3: Business user CANNOT downgrade to REGULAR (self)")
    public void testBusinessUserCannotDowngradeToRegular() {
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            userTypeChangeService.changeAccountType(businessUser, AccountType.REGULAR_USER,
                    null, "Test", null);
        });
        assertTrue(exception.getMessage().contains("Cannot downgrade"));
    }

    // ========== SCENARIO 4: Admin CAN Downgrade BUSINESS to REGULAR ==========
    @Test
    @DisplayName("SCENARIO 4: Admin CAN downgrade BUSINESS to REGULAR (for cleanup)")
    public void testAdminCanDowngradeBusinessToRegular() {
        userTypeChangeService.changeAccountType(businessUser, AccountType.REGULAR_USER,
                adminUser.getId(), "Admin cleanup", null);

        User updatedUser = userRepository.findById(businessUser.getId()).orElseThrow();
        assertEquals(AccountType.REGULAR_USER, updatedUser.getAccountType());
        assertEquals("USER", updatedUser.getAuthority().getAuthority());
    }

    // ========== SCENARIO 5: Non-existent Transition is Blocked ==========
    @Test
    @DisplayName("SCENARIO 5: Invalid transition throws exception")
    public void testInvalidTransitionIsBlocked() {
        // Try to change admin to regular (admin cannot change at all)
        assertThrows(AccessDeniedException.class, () -> {
            userTypeChangeService.changeAccountType(adminUser, AccountType.REGULAR_USER,
                    null, "Test", null);
        });
    }

    // ========== SCENARIO 6: Cannot Escalate to ADMIN ==========
    @Test
    @DisplayName("SCENARIO 6: Regular user cannot escalate to ADMIN")
    public void testEscalationToAdminIsBlocked() {
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            userTypeChangeService.changeAccountType(regularUser, AccountType.ADMIN,
                    null, "Escalation attempt", null);
        });
        assertEquals("Cannot promote user to admin via this endpoint.", exception.getMessage());
    }

    // ========== SCENARIO 7: Same Type Transition is Blocked ==========
    @Test
    @DisplayName("SCENARIO 7: User cannot change to same type")
    public void testSameTypeTransitionIsBlocked() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userTypeChangeService.changeAccountType(regularUser, AccountType.REGULAR_USER,
                    null, "Test", null);
        });
        assertTrue(exception.getMessage().contains("already"));
    }

    // ========== SCENARIO 8: Non-Admin Cannot Change Other User's Role ==========
    @Test
    @DisplayName("SCENARIO 8: Regular user cannot change another user's role")
    public void testNonAdminCannotChangeOtherUserRole() {
        // This should be handled in UserRestController layer, but validate service too
        // If changedByUserId is passed but user is not admin, service throws
        User nonAdminCaller = regularUser;

        // The controller should prevent this, but if service is called with non-admin
        // ID:
        // (service validates admin status)
        assertThrows(AccessDeniedException.class, () -> {
            userTypeChangeService.changeAccountType(businessUser, AccountType.REGULAR_USER,
                    nonAdminCaller.getId(), "Unauthorized attempt", null);
        });
    }

    // ========== SCENARIO 9: Authority is Updated Atomically ==========
    @Test
    @DisplayName("SCENARIO 9: Role transition updates authority atomically")
    public void testAuthorityUpdatesAtomically() {
        userTypeChangeService.changeAccountType(regularUser, AccountType.BUSINESS,
                adminUser.getId(), "Change for testing", null);

        User updatedUser = userRepository.findById(regularUser.getId()).orElseThrow();
        Authorities auth = updatedUser.getAuthority();

        assertEquals("BUSINESS", auth.getAuthority());
        assertEquals(AccountType.BUSINESS, updatedUser.getAccountType());
    }

    // ========== SCENARIO 10: Reverse Transition is Allowed ==========
    @Test
    @DisplayName("SCENARIO 10: Reverse transition (BUSINESS back to REGULAR) by admin is allowed")
    public void testReverseTransitionByAdmin() {
        // Upgrade USER to BUSINESS
        userTypeChangeService.changeAccountType(regularUser, AccountType.BUSINESS,
                adminUser.getId(), "Upgrade", null);

        User afterUpgrade = userRepository.findById(regularUser.getId()).orElseThrow();
        assertEquals(AccountType.BUSINESS, afterUpgrade.getAccountType());

        // Admin downgrades back to REGULAR
        userTypeChangeService.changeAccountType(afterUpgrade, AccountType.REGULAR_USER,
                adminUser.getId(), "Downgrade", null);

        User afterDowngrade = userRepository.findById(regularUser.getId()).orElseThrow();
        assertEquals(AccountType.REGULAR_USER, afterDowngrade.getAccountType());
        assertEquals("USER", afterDowngrade.getAuthority().getAuthority());
    }
}
