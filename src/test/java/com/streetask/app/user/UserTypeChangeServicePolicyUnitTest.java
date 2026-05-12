package com.streetask.app.user;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.streetask.app.business.BusinessAccount;
import com.streetask.app.exceptions.AccessDeniedException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@ExtendWith(MockitoExtension.class)
class UserTypeChangeServicePolicyUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthoritiesService authoritiesService;

    @Mock
    private UserRoleChangeLogRepository userRoleChangeLogRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @InjectMocks
    private UserTypeChangeService userTypeChangeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userTypeChangeService, "entityManager", entityManager);
    }

    @Test
    void regularUserCannotUpgradeToBusinessInSelfServiceFlow() {

        RegularUser regularUser = new RegularUser();
        regularUser.setAccountType(AccountType.REGULAR_USER);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> userTypeChangeService.changeAccountType(regularUser, AccountType.BUSINESS, null, "Self upgrade",
                        null));

        assertTrue(exception.getMessage().contains("cannot change their account type to Business"));
    }

    @Test
    void validateTransition_shouldThrowIfFromTypeIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userTypeChangeService.validateTransition(null, AccountType.BUSINESS, false));

        assertTrue(exception.getMessage().contains("Current account type cannot be null"));
    }

    @Test
    void validateTransition_shouldThrowIfToTypeIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userTypeChangeService.validateTransition(AccountType.REGULAR_USER, null, false));

        assertTrue(exception.getMessage().contains("New account type cannot be null"));
    }

    @Test
    @DisplayName("Coverage: Admin downgrades BUSINESS to REGULAR with null reason")
    void changeAccountType_adminDowngradeWithNullReason() {
        UUID adminId = UUID.randomUUID();
        User admin = new RegularUser();
        Authorities adminAuth = new Authorities();
        adminAuth.setAuthority("ADMIN");
        admin.setAuthority(adminAuth);

        RegularUser targetUser = new RegularUser();
        targetUser.setAccountType(AccountType.BUSINESS);
        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("BUSINESS");
        targetUser.setAuthority(userAuthority);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(0);

        userTypeChangeService.changeAccountType(targetUser, AccountType.REGULAR_USER, adminId, null, "127.0.0.1");

        verify(userRoleChangeLogRepository).save(any(UserRoleChangeLog.class));
    }

    @Test
    @DisplayName("Coverage: Admin upgrades REGULAR to BUSINESS")
    void changeAccountType_adminUpgrade() {
        UUID adminId = UUID.randomUUID();
        User admin = new RegularUser();
        Authorities adminAuth = new Authorities();
        adminAuth.setAuthority("ADMIN");
        admin.setAuthority(adminAuth);

        RegularUser targetUser = new RegularUser();
        targetUser.setAccountType(AccountType.REGULAR_USER);
        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");
        targetUser.setAuthority(userAuthority);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        Authorities returnedAuth = new Authorities();
        returnedAuth.setAuthority("BUSINESS");
        when(authoritiesService.findByAuthority("BUSINESS")).thenReturn(returnedAuth);

        userTypeChangeService.changeAccountType(targetUser, AccountType.BUSINESS, adminId, "Promotion", "127.0.0.1");

        verify(userRoleChangeLogRepository).save(any(UserRoleChangeLog.class));
    }

    @Test
    @DisplayName("Coverage: Self-service downgrade attempt throws Exception")
    void changeAccountType_selfDowngradeThrows() {
        RegularUser targetUser = new RegularUser();
        targetUser.setAccountType(AccountType.BUSINESS);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> userTypeChangeService.changeAccountType(targetUser, AccountType.REGULAR_USER, null, "Downgrade",
                        "127.0.0.1"));

        assertTrue(ex.getMessage().contains("Cannot downgrade from Business to Regular User"));
    }

    @Test
    void changeAccountType_selfSameRole() {
        RegularUser user = new RegularUser();
        user.setAccountType(AccountType.REGULAR_USER);

        assertThrows(IllegalArgumentException.class, () -> userTypeChangeService.changeAccountType(user,
                AccountType.REGULAR_USER, null, "razon", "127.0.0.1"));
    }

    @Test
    @DisplayName("Coverage: Admin downgrades with explicit reason")
    void changeAccountType_adminDowngradeWithReason() {
        UUID adminId = UUID.randomUUID();
        User admin = new RegularUser();
        Authorities adminAuth = new Authorities();
        adminAuth.setAuthority("ADMIN");
        admin.setAuthority(adminAuth);

        RegularUser targetUser = new RegularUser();
        targetUser.setAccountType(AccountType.BUSINESS);
        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("BUSINESS");
        targetUser.setAuthority(userAuthority);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        userTypeChangeService.changeAccountType(targetUser, AccountType.REGULAR_USER, adminId, "Valid Reason",
                "127.0.0.1");

        verify(userRoleChangeLogRepository).save(any(UserRoleChangeLog.class));
    }

    @Test
    @DisplayName("Coverage: convertToRegularUser with deleted records and null token")
    void changeAccountType_downgradeWithDeletedAttendancesAndNullToken() {
        UUID adminId = UUID.randomUUID();
        User admin = new RegularUser();
        Authorities adminAuth = new Authorities();
        adminAuth.setAuthority("ADMIN");
        admin.setAuthority(adminAuth);

        BusinessAccount targetUser = new BusinessAccount();
        targetUser.setId(UUID.randomUUID());
        targetUser.setAccountType(AccountType.BUSINESS);
        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("BUSINESS");
        targetUser.setAuthority(userAuthority);

        targetUser.setTokenVersion(null);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        Authorities returnedAuth = new Authorities();
        returnedAuth.setAuthority("USER");
        when(authoritiesService.findByAuthority("USER")).thenReturn(returnedAuth);

        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(3);

        userTypeChangeService.changeAccountType(targetUser, AccountType.REGULAR_USER, adminId, "Cleanup", "127.0.0.1");

        verify(userRepository).save(targetUser);
    }

    @Test
    @DisplayName("Coverage: convertToBusinessUser early return")
    void changeAccountType_upgradeAlreadyBusinessInstance() {
        UUID adminId = UUID.randomUUID();
        User admin = new RegularUser();
        Authorities adminAuth = new Authorities();
        adminAuth.setAuthority("ADMIN");
        admin.setAuthority(adminAuth);

        BusinessAccount targetUser = new BusinessAccount();
        targetUser.setAccountType(AccountType.REGULAR_USER);
        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");
        targetUser.setAuthority(userAuthority);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        userTypeChangeService.changeAccountType(targetUser, AccountType.BUSINESS, adminId, "Promoción", "127.0.0.1");
    }

    @Test
    @DisplayName("Coverage: convertToBusinessUser with null token")
    void changeAccountType_upgradeWithNullToken() {
        UUID adminId = UUID.randomUUID();
        User admin = new RegularUser();
        Authorities adminAuth = new Authorities();
        adminAuth.setAuthority("ADMIN");
        admin.setAuthority(adminAuth);

        RegularUser targetUser = new RegularUser();
        targetUser.setAccountType(AccountType.REGULAR_USER);
        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");
        targetUser.setAuthority(userAuthority);
        targetUser.setTokenVersion(null);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        Authorities businessAuth = new Authorities();
        businessAuth.setAuthority("BUSINESS");
        when(authoritiesService.findByAuthority("BUSINESS")).thenReturn(businessAuth);

        userTypeChangeService.changeAccountType(targetUser, AccountType.BUSINESS, adminId, "Promoción", "127.0.0.1");
    }

}
