package com.streetask.app.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.streetask.app.auth.payload.request.BusinessSignupRequest;
import com.streetask.app.auth.payload.request.CompleteSignupRequest;
import com.streetask.app.auth.payload.request.SignupRequest;
import com.streetask.app.auth.payload.response.JwtResponse;
import com.streetask.app.business.BusinessAccount;
import com.streetask.app.business.BusinessAccountRepository;
import com.streetask.app.business.RequestStatus;
import com.streetask.app.configuration.jwt.JwtUtils;
import com.streetask.app.user.AccountType;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.AuthoritiesService;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;
import com.streetask.app.user.UserService;
import com.streetask.app.user.UserTypeChangeService;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private AuthoritiesService authoritiesService;

    @Mock
    private UserService userService;

    @Mock
    private RegularUserRepository regularUserRepository;

    @Mock
    private BusinessAccountRepository businessAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserTypeChangeService userTypeChangeService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "entityManager", entityManager);
        ReflectionTestUtils.setField(authService, "mailFrom", "noreply@streetask.com");
    }

    @Test
    void createBasicUserShouldEncodePasswordSetDefaultsAssignUserAuthorityAndDelegatePersistence() {
        SignupRequest request = new SignupRequest();
        request.setEmail("basic@streetask.com");
        request.setUserName("basicUser");
        request.setPassword("plain-password");
        request.setFirstName("Basic");
        request.setLastName("User");

        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");

        User savedUser = new User();
        savedUser.setEmail("basic@streetask.com");

        when(encoder.encode("plain-password")).thenReturn("encoded-password");
        when(authoritiesService.findByAuthority("USER")).thenReturn(userAuthority);
        when(userService.saveUser(any(User.class))).thenReturn(savedUser);

        User result = authService.createBasicUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).saveUser(userCaptor.capture());

        User userToSave = userCaptor.getValue();
        assertThat(userToSave.getEmail()).isEqualTo("basic@streetask.com");
        assertThat(userToSave.getUserName()).isEqualTo("basicUser");
        assertThat(userToSave.getPassword()).isEqualTo("encoded-password");
        assertThat(userToSave.getFirstName()).isEqualTo("Basic");
        assertThat(userToSave.getLastName()).isEqualTo("User");
        assertThat(userToSave.getActive()).isFalse();
        assertThat(userToSave.getCreatedAt()).isNotNull();
        assertThat(userToSave.getAuthority()).isEqualTo(userAuthority);

        verify(encoder).encode("plain-password");
        verify(authoritiesService).findByAuthority("USER");
        assertThat(result).isEqualTo(savedUser);
    }

    @Test
    void createRegularUserShouldCopyBaseFieldsSetDefaultsDeleteBasicUserFlushAndSaveRegularUser() {
        CompleteSignupRequest request = new CompleteSignupRequest();
        request.setEmail("regular@streetask.com");

        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");

        User basicUser = new User();
        basicUser.setId(UUID.randomUUID());
        basicUser.setEmail("regular@streetask.com");
        basicUser.setUserName("regularUser");
        basicUser.setPassword("encoded-password");
        basicUser.setFirstName("Regular");
        basicUser.setLastName("User");
        basicUser.setCreatedAt(LocalDateTime.of(2026, 3, 10, 12, 0));

        when(userService.findUser("regular@streetask.com")).thenReturn(basicUser);
        when(authoritiesService.findByAuthority("USER")).thenReturn(userAuthority);

        authService.createRegularUser(request);

        ArgumentCaptor<RegularUser> regularUserCaptor = ArgumentCaptor.forClass(RegularUser.class);
        verify(regularUserRepository).save(regularUserCaptor.capture());

        RegularUser savedRegularUser = regularUserCaptor.getValue();
        assertThat(savedRegularUser.getEmail()).isEqualTo(basicUser.getEmail());
        assertThat(savedRegularUser.getUserName()).isEqualTo(basicUser.getUserName());
        assertThat(savedRegularUser.getPassword()).isEqualTo(basicUser.getPassword());
        assertThat(savedRegularUser.getFirstName()).isEqualTo(basicUser.getFirstName());
        assertThat(savedRegularUser.getLastName()).isEqualTo(basicUser.getLastName());
        assertThat(savedRegularUser.getCreatedAt()).isEqualTo(basicUser.getCreatedAt());

        assertThat(savedRegularUser.getAccountType()).isEqualTo(AccountType.REGULAR_USER);
        assertThat(savedRegularUser.getActive()).isTrue();
        assertThat(savedRegularUser.getCoinBalance()).isEqualTo(0);
        assertThat(savedRegularUser.getRating()).isEqualTo(0.0f);
        assertThat(savedRegularUser.getVerified()).isFalse();
        assertThat(savedRegularUser.getVisibilityRadiusKm()).isEqualTo(10.0f);
        assertThat(savedRegularUser.getPremiumActive()).isFalse();
        assertThat(savedRegularUser.getAuthority()).isEqualTo(userAuthority);

        verify(userService).findUser("regular@streetask.com");
        verify(authoritiesService).findByAuthority("USER");

        var inOrder = inOrder(userService, entityManager, regularUserRepository);
        inOrder.verify(userService).deleteUser(basicUser.getId());
        inOrder.verify(entityManager).flush();
        inOrder.verify(regularUserRepository).save(any(RegularUser.class));
    }

    @Test
    void convertToBusinessUserShouldCopyBaseFieldsSetBusinessFieldsDeleteBasicUserFlushAndSaveBusinessUser() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("business@streetask.com");
        request.setTaxId("B12345678");
        request.setCompanyName("StreetAsk Business");
        request.setAddress("Calle Real 123");
        request.setWebsite("https://streetask-business.com");
        request.setDescription("Business description");

        Authorities businessAuthority = new Authorities();
        businessAuthority.setAuthority("BUSINESS");

        User basicUser = new User();
        basicUser.setId(UUID.randomUUID());
        basicUser.setEmail("business@streetask.com");
        basicUser.setUserName("businessUser");
        basicUser.setPassword("encoded-password");
        basicUser.setFirstName("Business");
        basicUser.setLastName("Owner");
        basicUser.setCreatedAt(LocalDateTime.of(2026, 3, 10, 10, 30));
        basicUser.setActive(false);

        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");
        basicUser.setAuthority(userAuthority);

        when(userService.findUser("business@streetask.com")).thenReturn(basicUser);
        when(authoritiesService.findByAuthority("BUSINESS")).thenReturn(businessAuthority);

        authService.convertToBusinessUser(request);

        ArgumentCaptor<BusinessAccount> businessCaptor = ArgumentCaptor.forClass(BusinessAccount.class);
        verify(businessAccountRepository).save(businessCaptor.capture());

        BusinessAccount savedBusiness = businessCaptor.getValue();
        assertThat(savedBusiness.getEmail()).isEqualTo(basicUser.getEmail());
        assertThat(savedBusiness.getUserName()).isEqualTo(basicUser.getUserName());
        assertThat(savedBusiness.getPassword()).isEqualTo(basicUser.getPassword());
        assertThat(savedBusiness.getFirstName()).isEqualTo(basicUser.getFirstName());
        assertThat(savedBusiness.getLastName()).isEqualTo(basicUser.getLastName());
        assertThat(savedBusiness.getCreatedAt()).isEqualTo(basicUser.getCreatedAt());

        assertThat(savedBusiness.getAccountType()).isEqualTo(AccountType.BUSINESS);
        assertThat(savedBusiness.getActive()).isFalse();

        assertThat(savedBusiness.getTaxId()).isEqualTo("B12345678");
        assertThat(savedBusiness.getCompanyName()).isEqualTo("StreetAsk Business");
        assertThat(savedBusiness.getAddress()).isEqualTo("Calle Real 123");
        assertThat(savedBusiness.getWebsite()).isEqualTo("https://streetask-business.com");
        assertThat(savedBusiness.getDescription()).isEqualTo("Business description");

        assertThat(savedBusiness.getVerified()).isFalse();
        assertThat(savedBusiness.getRating()).isEqualTo(0.0f);
        assertThat(savedBusiness.getRequestStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(savedBusiness.getSubscriptionActive()).isFalse();
        assertThat(savedBusiness.getAuthority()).isEqualTo(businessAuthority);

        verify(userService).findUser("business@streetask.com");
        verify(authoritiesService).findByAuthority("BUSINESS");

        var inOrder = inOrder(userService, entityManager, businessAccountRepository);
        inOrder.verify(userService).deleteUser(basicUser.getId());
        inOrder.verify(entityManager).flush();
        inOrder.verify(businessAccountRepository).save(any(BusinessAccount.class));
    }

    @Test
    void convertToBusinessUserShouldRejectWhenUserIsAlreadyBusinessAccount() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("business@streetask.com");
        request.setTaxId("B12345678");
        request.setCompanyName("StreetAsk Business");
        request.setAddress("Calle Real 123");

        BusinessAccount existingBusiness = new BusinessAccount();
        existingBusiness.setId(UUID.randomUUID());
        existingBusiness.setEmail("business@streetask.com");
        existingBusiness.setUserName("businessUser");
        existingBusiness.setPassword("encoded-password");
        existingBusiness.setFirstName("Business");
        existingBusiness.setLastName("Owner");
        existingBusiness.setCreatedAt(LocalDateTime.of(2026, 3, 10, 10, 30));

        when(userService.findUser("business@streetask.com")).thenReturn(existingBusiness);

        assertThatThrownBy(() -> authService.convertToBusinessUser(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is already a business account.");

        verify(userService).findUser("business@streetask.com");
        verify(userService, never()).deleteUser(any());
        verify(businessAccountRepository, never()).save(any(BusinessAccount.class));
    }

    @Test
    void requestPasswordResetShouldReturnEarlyWhenEmailIsInvalid() {
        authService.requestPasswordReset(null);
        authService.requestPasswordReset("   ");

        verify(userRepository, never()).findByEmailIgnoreCase(any());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void requestPasswordResetShouldReturnEarlyWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing@streetask.com")).thenReturn(Optional.empty());

        authService.requestPasswordReset("missing@streetask.com");

        verify(userRepository).findByEmailIgnoreCase("missing@streetask.com");
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void requestPasswordResetShouldCreateTokenAndUseFallbackMailWhenHtmlSendFails() {
        User user = new User();
        user.setEmail("user@streetask.com");

        when(userRepository.findByEmailIgnoreCase("user@streetask.com")).thenReturn(Optional.of(user));
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("smtp html failure"));

        authService.requestPasswordReset("user@streetask.com");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getToken()).isNotBlank();
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(25));
        assertThat(savedToken.getUsedAt()).isNull();

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getTo()).containsExactly("user@streetask.com");
    }

    @Test
    void requestPasswordResetShouldUseHtmlMailWhenMimeSendSucceeds() {
        User user = new User();
        user.setEmail("html@streetask.com");

        when(userRepository.findByEmailIgnoreCase("html@streetask.com")).thenReturn(Optional.of(user));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        authService.requestPasswordReset("html@streetask.com");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void resetPasswordShouldReturnFalseWhenInputIsInvalid() {
        assertThat(authService.resetPassword(null, "newPass")).isFalse();
        assertThat(authService.resetPassword(" ", "newPass")).isFalse();
        assertThat(authService.resetPassword("token", null)).isFalse();
        assertThat(authService.resetPassword("token", " ")).isFalse();

        verify(passwordResetTokenRepository, never()).findByToken(any());
    }

    @Test
    void resetPasswordShouldReturnFalseWhenTokenDoesNotExist() {
        when(passwordResetTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        boolean result = authService.resetPassword("unknown", "newPass");

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPasswordShouldReturnFalseWhenTokenIsUsed() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsedAt(LocalDateTime.now().minusMinutes(1));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        boolean result = authService.resetPassword("used-token", "newPass");

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPasswordShouldReturnFalseWhenTokenIsExpired() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsedAt(null);
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        boolean result = authService.resetPassword("expired-token", "newPass");

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPasswordShouldUpdatePasswordAndMarkTokenAsUsedWhenTokenIsValid() {
        User user = new User();
        user.setEmail("valid@streetask.com");

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setUsedAt(null);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(encoder.encode("newPass")).thenReturn("encoded-new-pass");

        boolean result = authService.resetPassword("valid-token", "newPass");

        assertThat(result).isTrue();
        assertThat(user.getPassword()).isEqualTo("encoded-new-pass");
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void isPendingBasicSignupShouldReturnFalseForInvalidIdentifier() {
        assertThat(authService.isPendingBasicSignup(null)).isFalse();
        assertThat(authService.isPendingBasicSignup(" ")).isFalse();
        verify(userRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void isPendingBasicSignupShouldReturnTrueUsingUsernameFallback() {
        User pendingUser = new User();
        pendingUser.setAccountType(null);
        pendingUser.setActive(false);
        Authorities authority = new Authorities();
        authority.setAuthority("USER");
        pendingUser.setAuthority(authority);

        when(userRepository.findByEmailIgnoreCase("pending-user")).thenReturn(Optional.empty());
        when(userRepository.findByUserNameIgnoreCase("pending-user")).thenReturn(Optional.of(pendingUser));

        boolean result = authService.isPendingBasicSignup("pending-user");

        assertThat(result).isTrue();
    }

    @Test
    void isPendingBasicSignupShouldReturnFalseWhenRepositoryThrows() {
        when(userRepository.findByEmailIgnoreCase("boom")).thenThrow(new RuntimeException("db down"));

        boolean result = authService.isPendingBasicSignup("boom");

        assertThat(result).isFalse();
    }

    @Test
    void isPendingBasicSignupShouldReturnFalseWhenResolvedUserIsNotPending() {
        User existingUser = new User();
        existingUser.setActive(true);

        when(userRepository.findByEmailIgnoreCase("existing@streetask.com")).thenReturn(Optional.of(existingUser));

        boolean result = authService.isPendingBasicSignup("existing@streetask.com");

        assertThat(result).isFalse();
    }

    @Test
    void getPendingBasicSignupEmailShouldReturnNullWhenIdentifierIsInvalid() {
        assertThat(authService.getPendingBasicSignupEmail(null)).isNull();
        assertThat(authService.getPendingBasicSignupEmail(" ")).isNull();
        verify(userRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void getPendingBasicSignupEmailShouldResolveEmailUsingUsernameFallback() {
        User pendingUser = new User();
        pendingUser.setEmail("pending@streetask.com");

        when(userRepository.findByEmailIgnoreCase("pending-user")).thenReturn(Optional.empty());
        when(userRepository.findByUserNameIgnoreCase("pending-user")).thenReturn(Optional.of(pendingUser));

        String result = authService.getPendingBasicSignupEmail("pending-user");

        assertThat(result).isEqualTo("pending@streetask.com");
    }

    @Test
    void getPendingBasicSignupEmailShouldReturnNullWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing")).thenReturn(Optional.empty());
        when(userRepository.findByUserNameIgnoreCase("missing")).thenReturn(Optional.empty());

        String result = authService.getPendingBasicSignupEmail("missing");

        assertThat(result).isNull();
    }

    @Test
    void getPendingBasicSignupEmailShouldReturnNullWhenRepositoryThrows() {
        when(userRepository.findByEmailIgnoreCase("boom")).thenThrow(new RuntimeException("db down"));

        String result = authService.getPendingBasicSignupEmail("boom");

        assertThat(result).isNull();
    }

    @Test
    void validatePendingBasicSignupShouldThrowExceptionWhenUserIsInvalid() {
        User pendingUser = new User();
        pendingUser.setEmail("pending@streetask.com");
        pendingUser.setActive(true);
        pendingUser.setAccountType(null);
        Authorities authority = new Authorities();
        authority.setAuthority("USER");
        pendingUser.setAuthority(authority);

        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("pending@streetask.com");
        when(userService.findUser("pending@streetask.com")).thenReturn(pendingUser);

        assertThatThrownBy(() -> authService.convertToBusinessUser(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is not eligible for business signup.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void isPendingBasicSignupShouldReturnFalseWhenUserIsNull() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("pending@streetask.com");

        when(userService.findUser("pending@streetask.com")).thenReturn(null);

        assertThatThrownBy(() -> authService.convertToBusinessUser(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is not eligible for business signup.");
    }

    @Test
    void isPendingBasicSignupShouldReturnFalseWhenAnyConditionFails() {
        String email = "pending@streetask.com";

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        when(userRepository.findByUserNameIgnoreCase(email)).thenReturn(Optional.empty());
        assertThat(authService.isPendingBasicSignup(email)).isFalse();

        User user = new User();
        user.setAccountType(null);
        user.setActive(false);
        Authorities authority = new Authorities();
        authority.setAuthority("USER");
        user.setAuthority(authority);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        user.setAccountType(AccountType.REGULAR_USER);
        assertThat(authService.isPendingBasicSignup(email)).isFalse();
        user.setAccountType(null);

        user.setActive(true);
        assertThat(authService.isPendingBasicSignup(email)).isFalse();
        user.setActive(false);

        authority.setAuthority("BUSINESS");
        assertThat(authService.isPendingBasicSignup(email)).isFalse();
    }

    @Test
    void changeUserAccountTypeShouldReturnJwtResponseWithUpdatedRoles() {
        UUID userId = UUID.randomUUID();
        UUID changedBy = UUID.randomUUID();

        User original = new User();
        original.setId(userId);
        original.setEmail("user@streetask.com");

        User updated = new User();
        updated.setId(userId);
        updated.setEmail("user@streetask.com");
        Authorities authority = new Authorities();
        authority.setAuthority("BUSINESS");
        updated.setAuthority(authority);

        when(userService.findUser(userId)).thenReturn(original, updated);
        when(jwtUtils.generateJwtToken(any())).thenReturn("jwt-new-token");

        JwtResponse response = authService.changeUserAccountType(userId, AccountType.BUSINESS, changedBy,
                "requested change", "127.0.0.1");

        verify(userTypeChangeService).changeAccountType(eq(original), eq(AccountType.BUSINESS), eq(changedBy),
                eq("requested change"), eq("127.0.0.1"));
        assertThat(response.getToken()).isEqualTo("jwt-new-token");
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo("user@streetask.com");
        assertThat(response.getRoles()).containsExactly("BUSINESS");
    }
}
