package com.streetask.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

import com.streetask.app.auth.payload.request.LoginRequest;
import com.streetask.app.auth.payload.request.BusinessSignupRequest;
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
    void completeBusinessUserShouldReturnBadRequestWhenServiceThrowsException() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("business@streetask.com");
        request.setTaxId("B12345678");
        request.setCompanyName("StreetAsk Business");
        request.setAddress("Address 1");

        when(userService.existsUser("business@streetask.com")).thenReturn(true);
        when(businessAccountRepository.existsByTaxId("B12345678")).thenReturn(false);
        doThrow(new RuntimeException("Unexpected conversion error")).when(authService).convertToBusinessUser(any());

        ResponseEntity<MessageResponse> response = authController.completeBusinessUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Error: User not found or already completed!");
    }

    @Test
    void completeBusinessUserShouldReturnBadRequestWhenUserIsAlreadyBusinessAccount() {
        BusinessSignupRequest request = new BusinessSignupRequest();
        request.setEmail("business@streetask.com");
        request.setTaxId("B12345678");
        request.setCompanyName("StreetAsk Business");
        request.setAddress("Address 1");

        when(userService.existsUser("business@streetask.com")).thenReturn(true);
        when(businessAccountRepository.existsByTaxId("B12345678")).thenReturn(false);
        doThrow(new IllegalStateException("User is already a business account.")).when(authService)
                .convertToBusinessUser(any());

        ResponseEntity<MessageResponse> response = authController.completeBusinessUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Error: User is already a business account.");
    }
}
