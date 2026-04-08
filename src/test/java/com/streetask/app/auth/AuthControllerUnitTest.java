package com.streetask.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.streetask.app.auth.payload.request.BusinessSignupRequest;
import com.streetask.app.auth.payload.request.LoginRequest;
import com.streetask.app.auth.payload.response.JwtResponse;
import com.streetask.app.auth.payload.response.MessageResponse;
import com.streetask.app.configuration.jwt.JwtUtils;
import com.streetask.app.configuration.services.UserDetailsImpl;
import com.streetask.app.user.BusinessAccountRepository;
import com.streetask.app.user.UserService;

@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

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

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(
                authenticationManager,
                userService,
                jwtUtils,
                authService,
                businessAccountRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticateUserShouldReturnBadRequestWhenIdentifierIsBlank() {
        LoginRequest request = new LoginRequest();
        request.setEmail("   ");
        request.setPassword("123456");

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(MessageResponse.class);
        MessageResponse body = (MessageResponse) response.getBody();
        assertThat(body.getMessage()).isEqualTo("Error: Email/username and password are required.");
    }

    @Test
    void authenticateUserShouldReturnBadRequestWhenPasswordIsNull() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin1");
        request.setPassword(null);

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(MessageResponse.class);
        MessageResponse body = (MessageResponse) response.getBody();
        assertThat(body.getMessage()).isEqualTo("Error: Email/username and password are required.");
    }

    @Test
    void authenticateUserShouldReturnBadRequestWhenPasswordIsBlank() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin1");
        request.setPassword("   ");

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(MessageResponse.class);
        MessageResponse body = (MessageResponse) response.getBody();
        assertThat(body.getMessage()).isEqualTo("Error: Email/username and password are required.");
    }

    @Test
    void authenticateUserShouldReturnUnauthorizedWhenAuthenticationFails() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin1");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(MessageResponse.class);
        MessageResponse body = (MessageResponse) response.getBody();
        assertThat(body.getMessage()).isEqualTo("Error: Invalid email or password.");
    }

    @Test
    void authenticateUserShouldReturnJwtWhenAuthenticationSucceeds() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin1");
        request.setPassword("4dm1n");

        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "admin1@streetask.com",
                "encoded-password",
                List.of(new SimpleGrantedAuthority("ADMIN")));

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.authenticateUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(JwtResponse.class);

        JwtResponse body = (JwtResponse) response.getBody();
        assertThat(body.getToken()).isEqualTo("jwt-token");
        assertThat(body.getUsername()).isEqualTo("admin1@streetask.com");
        assertThat(body.getRoles()).containsExactly("ADMIN");
    }

    @Test
    void completeBusinessUserShouldReturnBadRequestWhenBasicUserRegistrationDoesNotExist() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("missing.business@streetask.com");
        request.setTaxId("B12345678");
        request.setCompanyName("StreetAsk Missing Co");

        when(userService.existsUser("missing.business@streetask.com")).thenReturn(false);

        ResponseEntity<MessageResponse> response = authController.completeBusinessUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Error: Basic user registration not found. Please complete the basic signup first.");
        verifyNoInteractions(businessAccountRepository, authService);
    }

    @Test
    void completeBusinessUserShouldReturnBadRequestWhenTaxIdAlreadyExistsAfterNormalization() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("business.dup@streetask.com");
        request.setTaxId("b-1234 5678");
        request.setCompanyName("StreetAsk Dup Co");

        when(userService.existsUser("business.dup@streetask.com")).thenReturn(true);
        when(businessAccountRepository.existsByTaxId("B12345678")).thenReturn(true);

        ResponseEntity<MessageResponse> response = authController.completeBusinessUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Error: Tax ID is already registered!");
        assertThat(request.getTaxId()).isEqualTo("B12345678");
        verify(userService).existsUser("business.dup@streetask.com");
        verify(businessAccountRepository).existsByTaxId("B12345678");
        verifyNoInteractions(authService);
    }

    @Test
    void completeBusinessUserShouldNormalizeTaxIdAndCallServiceWhenPayloadIsValid() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("business.ok@streetask.com");
        request.setTaxId("b-1234 5670");
        request.setCompanyName("StreetAsk Ok Co");

        when(userService.existsUser("business.ok@streetask.com")).thenReturn(true);
        when(businessAccountRepository.existsByTaxId("B12345670")).thenReturn(false);

        ResponseEntity<MessageResponse> response = authController.completeBusinessUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Business account registered successfully! Your account is pending admin verification.");
        assertThat(request.getTaxId()).isEqualTo("B12345670");
        verify(userService).existsUser("business.ok@streetask.com");
        verify(businessAccountRepository).existsByTaxId("B12345670");
        verify(authService).convertToBusinessUser(eq(request));
    }

    @Test
    void completeBusinessUserShouldReturnBadRequestWhenServiceThrowsException() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("business.fail@streetask.com");
        request.setTaxId("B12345671");
        request.setCompanyName("StreetAsk Fail Co");

        when(userService.existsUser("business.fail@streetask.com")).thenReturn(true);
        when(businessAccountRepository.existsByTaxId("B12345671")).thenReturn(false);
        doThrow(new IllegalStateException("boom")).when(authService).convertToBusinessUser(any(BusinessSignupRequest.class));

        ResponseEntity<MessageResponse> response = authController.completeBusinessUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Error: User not found or already completed!");
        verify(authService).convertToBusinessUser(eq(request));
    }
}
