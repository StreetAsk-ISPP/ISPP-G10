package com.streetask.app.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.streetask.app.auth.AuthController;
import com.streetask.app.auth.AuthService;
import com.streetask.app.auth.PasswordResetTokenRepository;
import com.streetask.app.auth.payload.request.BusinessSignupRequest;
import com.streetask.app.auth.payload.response.MessageResponse;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.AuthoritiesService;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;
import com.streetask.app.user.UserService;
import com.streetask.app.user.RegularUserRepository;
import com.streetask.app.configuration.jwt.JwtUtils;

@ExtendWith(MockitoExtension.class)
class BusinessAccountCreationUnitTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthService authService;

    @Mock
    private BusinessAccountRepository businessAccountRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private AuthoritiesService authoritiesService;

    @Mock
    private RegularUserRepository regularUserRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AuthService authServiceForCreation;

    private AuthController authController;
    private Validator validator;

    @BeforeEach
    void setUp() {
        authController = new AuthController(
                authenticationManager,
                userService,
                jwtUtils,
                authService,
                businessAccountRepository);

        ReflectionTestUtils.setField(authServiceForCreation, "entityManager", entityManager);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validBusinessDataCreatesAccountSuccessfully() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("business.ok@streetask.com");
        request.setTaxId("b-1234 5670");
        request.setCompanyName("StreetAsk Business");
        request.setAddress("Main Street 123");

        when(authService.isPendingBasicSignup("business.ok@streetask.com")).thenReturn(true);
        when(businessAccountRepository.existsByTaxId("B12345670")).thenReturn(false);

        ResponseEntity<MessageResponse> response = authController.completeBusinessUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Business account registered successfully! Your account is pending admin verification.");
        assertThat(request.getTaxId()).isEqualTo("B12345670");

        verify(authService).convertToBusinessUser(eq(request));
    }

    @Test
    void invalidNifFormatIsRejected() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("business.badnif@streetask.com");
        request.setTaxId("12345678A");
        request.setCompanyName("StreetAsk Business");

        Set<ConstraintViolation<BusinessSignupRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> "taxId".equals(v.getPropertyPath().toString()));
    }

    @Test
    void duplicateNifIsRejected() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("business.dup@streetask.com");
        request.setTaxId("b-1234 5678");
        request.setCompanyName("StreetAsk Duplicated");

        when(authService.isPendingBasicSignup("business.dup@streetask.com")).thenReturn(true);
        when(businessAccountRepository.existsByTaxId("B12345678")).thenReturn(true);

        ResponseEntity<MessageResponse> response = authController.completeBusinessUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Error: Tax ID is already registered!");
        verify(authService).isPendingBasicSignup("business.dup@streetask.com");
        verify(authService, never()).convertToBusinessUser(any(BusinessSignupRequest.class));
    }

    @Test
    void missingRequiredFieldsReturnValidationErrors() {
        BusinessSignupRequest request = new BusinessSignupRequest();

        Set<ConstraintViolation<BusinessSignupRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> "email".equals(v.getPropertyPath().toString()));
        assertThat(violations)
                .anyMatch(v -> "taxId".equals(v.getPropertyPath().toString()));
        assertThat(violations)
                .anyMatch(v -> "companyName".equals(v.getPropertyPath().toString()));
    }

    @Test
    void newBusinessAccountsAreCreatedWithPendingStatus() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("business.pending@streetask.com");
        request.setTaxId("B12345679");
        request.setCompanyName("StreetAsk Pending");

        User basicUser = new User();
        basicUser.setId(UUID.randomUUID());
        basicUser.setEmail("business.pending@streetask.com");
        basicUser.setUserName("businessPending");
        basicUser.setPassword("encoded-password");
        basicUser.setFirstName("Pending");
        basicUser.setLastName("Owner");
        basicUser.setAccountType(null);
        basicUser.setActive(false);
        basicUser.setCreatedAt(LocalDateTime.of(2026, 4, 8, 12, 0));
        basicUser.setActive(false);

        Authorities userAuthority = new Authorities();
        userAuthority.setAuthority("USER");
        basicUser.setAuthority(userAuthority);

        Authorities basicAuthority = new Authorities();
        basicAuthority.setAuthority("USER");

        Authorities businessAuthority = new Authorities();
        businessAuthority.setAuthority("BUSINESS");

        basicUser.setAuthority(basicAuthority);

        when(userService.findUser("business.pending@streetask.com")).thenReturn(basicUser);
        when(authoritiesService.findByAuthority("BUSINESS")).thenReturn(businessAuthority);

        authServiceForCreation.convertToBusinessUser(request);

        ArgumentCaptor<BusinessAccount> businessCaptor = ArgumentCaptor.forClass(BusinessAccount.class);
        verify(businessAccountRepository).save(businessCaptor.capture());

        BusinessAccount savedBusiness = businessCaptor.getValue();
        assertThat(savedBusiness.getRequestStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(savedBusiness.getVerified()).isFalse();
        assertThat(savedBusiness.getActive()).isFalse();
        assertThat(savedBusiness.getTaxId()).isEqualTo("B12345679");
        assertThat(savedBusiness.getCompanyName()).isEqualTo("StreetAsk Pending");

        verify(entityManager).flush();
        verify(userService).deleteUser(basicUser.getId());
        verify(businessAccountRepository).save(any(BusinessAccount.class));
    }
}
