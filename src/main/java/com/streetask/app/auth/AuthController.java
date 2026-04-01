package com.streetask.app.auth;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.streetask.app.auth.payload.request.BusinessSignupRequest;
import com.streetask.app.auth.payload.request.CompleteSignupRequest;
import com.streetask.app.auth.payload.request.ForgotPasswordRequest;
import com.streetask.app.auth.payload.request.LoginRequest;
import com.streetask.app.auth.payload.request.ResetPasswordRequest;
import com.streetask.app.auth.payload.request.SignupRequest;
import com.streetask.app.auth.payload.response.JwtResponse;
import com.streetask.app.auth.payload.response.MessageResponse;
import com.streetask.app.configuration.jwt.JwtUtils;
import com.streetask.app.configuration.services.UserDetailsImpl;
import com.streetask.app.business.BusinessAccountRepository;
import com.streetask.app.user.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "The Authentication API based on JWT")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final UserService userService;
	private final JwtUtils jwtUtils;
	private final AuthService authService;
	private final BusinessAccountRepository businessAccountRepository;

	@Autowired
	public AuthController(AuthenticationManager authenticationManager, UserService userService, JwtUtils jwtUtils,
			AuthService authService, BusinessAccountRepository businessAccountRepository) {
		this.userService = userService;
		this.jwtUtils = jwtUtils;
		this.authenticationManager = authenticationManager;
		this.authService = authService;
		this.businessAccountRepository = businessAccountRepository;
	}

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
		String normalizedIdentifier = loginRequest.getEmail().trim();
		String rawPassword = loginRequest.getPassword();

		if (normalizedIdentifier.isBlank() || rawPassword == null || rawPassword.isBlank()) {
			return ResponseEntity.badRequest()
					.body(new MessageResponse("Error: Email/username and password are required."));
		}

		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(normalizedIdentifier, rawPassword));

			SecurityContextHolder.getContext().setAuthentication(authentication);
			String jwt = jwtUtils.generateJwtToken(authentication);

			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
			List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
					.collect(Collectors.toList());

			return ResponseEntity.ok()
					.body(new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), roles));
		} catch (AuthenticationException exception) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new MessageResponse("Error: Invalid email or password."));
		}
	}

	@GetMapping("/validate")
	public ResponseEntity<Boolean> validateToken(@RequestParam String token) {
		Boolean isValid = jwtUtils.validateJwtToken(token);
		return ResponseEntity.ok(isValid);
	}

	@PostMapping("/signup/basic")
	public ResponseEntity<MessageResponse> registerBasicUser(@Valid @RequestBody SignupRequest signUpRequest) {
		// Check whether email already exists
		if (userService.existsUser(signUpRequest.getEmail()).equals(true)) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already registered!"));
		}
		// Check whether username already exists
		if (userService.existsByUserName(signUpRequest.getUserName()).equals(true)) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
		}
		authService.createBasicUser(signUpRequest);
		return ResponseEntity.ok(new MessageResponse("Basic user data saved! Complete your registration."));
	}

	@PostMapping("/signup/regular")
	public ResponseEntity<MessageResponse> completeRegularUser(
			@Valid @RequestBody CompleteSignupRequest signUpRequest) {
		// Check whether the basic user exists
		try {
			authService.createRegularUser(signUpRequest);
			return ResponseEntity.ok(new MessageResponse("Regular user registered successfully!"));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found or already completed!"));
		}
	}

	@PostMapping("/signup/business")
	public ResponseEntity<MessageResponse> completeBusinessUser(
			@Valid @RequestBody BusinessSignupRequest signUpRequest) {
		// Validate email exists and is a basic user
		if (!userService.existsUser(signUpRequest.getEmail())) {
			return ResponseEntity.badRequest()
					.body(new MessageResponse(
							"Error: Basic user registration not found. Please complete the basic signup first."));
		}

		String normalizedTaxId = signUpRequest.getTaxId().trim().toUpperCase().replace(" ", "").replace("-", "");
		signUpRequest.setTaxId(normalizedTaxId);

		// Check whether tax ID already exists
		if (businessAccountRepository.existsByTaxId(signUpRequest.getTaxId())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Tax ID is already registered!"));
		}
		// Complete business account data
		try {
			authService.convertToBusinessUser(signUpRequest);
			return ResponseEntity.ok(new MessageResponse(
					"Business account registered successfully! Your account is pending admin verification."));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: User not found or already completed!"));
		}
	}

	@PostMapping("/password/forgot")
	public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.requestPasswordReset(request.getEmail());
		return ResponseEntity.ok(new MessageResponse(
				"If the email exists in our system, you will receive password reset instructions shortly."));
	}

	@PostMapping("/password/reset")
	public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		boolean updated = authService.resetPassword(request.getToken(), request.getNewPassword());
		if (!updated) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new MessageResponse("Error: Reset token is invalid or expired."));
		}

		return ResponseEntity.ok(new MessageResponse("Password updated successfully."));
	}

}
