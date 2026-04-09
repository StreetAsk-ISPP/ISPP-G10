package com.streetask.app.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import com.streetask.app.auth.payload.request.BusinessSignupRequest;
import com.streetask.app.auth.payload.request.CompleteSignupRequest;
import com.streetask.app.auth.payload.request.SignupRequest;
import com.streetask.app.business.BusinessAccount;
import com.streetask.app.business.BusinessAccountRepository;
import com.streetask.app.business.RequestStatus;
import com.streetask.app.user.Authorities;
import com.streetask.app.user.AuthoritiesService;
import com.streetask.app.user.AccountType;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;
import com.streetask.app.user.UserService;
import com.streetask.app.user.RegularUser;
import com.streetask.app.user.RegularUserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class AuthService {

	private static final float DEFAULT_VISIBILITY_RADIUS_KM = 10.0f;
	private static final int PASSWORD_RESET_TOKEN_MINUTES = 30;

	@PersistenceContext
	private EntityManager entityManager;

	private final PasswordEncoder encoder;
	private final AuthoritiesService authoritiesService;
	private final UserService userService;
	private final RegularUserRepository regularUserRepository;
	private final BusinessAccountRepository businessAccountRepository;
	private final UserRepository userRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final JavaMailSender mailSender;

	@Value("${streetask.mail.from}")
	private String mailFrom;

	@Autowired
	public AuthService(PasswordEncoder encoder, AuthoritiesService authoritiesService, UserService userService,
			RegularUserRepository regularUserRepository, BusinessAccountRepository businessAccountRepository,
			UserRepository userRepository, PasswordResetTokenRepository passwordResetTokenRepository,
			JavaMailSender mailSender) {
		this.encoder = encoder;
		this.authoritiesService = authoritiesService;
		this.userService = userService;
		this.regularUserRepository = regularUserRepository;
		this.businessAccountRepository = businessAccountRepository;
		this.userRepository = userRepository;
		this.passwordResetTokenRepository = passwordResetTokenRepository;
		this.mailSender = mailSender;
	}

	@Transactional
	public void requestPasswordReset(String email) {
		if (email == null || email.isBlank()) {
			return;
		}

		Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email.trim());
		if (userOpt.isEmpty()) {
			return;
		}

		User user = userOpt.get();
		passwordResetTokenRepository.deleteByUser(user);

		PasswordResetToken resetToken = new PasswordResetToken();
		resetToken.setUser(user);
		resetToken.setToken(UUID.randomUUID().toString());
		resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(PASSWORD_RESET_TOKEN_MINUTES));
		resetToken.setUsedAt(null);

		passwordResetTokenRepository.save(resetToken);
		sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
	}

	@Transactional
	public boolean resetPassword(String token, String newPassword) {
		if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
			return false;
		}

		Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token.trim());
		if (tokenOpt.isEmpty()) {
			return false;
		}

		PasswordResetToken resetToken = tokenOpt.get();
		if (resetToken.getUsedAt() != null || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			return false;
		}

		User user = resetToken.getUser();
		user.setPassword(encoder.encode(newPassword));
		userRepository.save(user);

		resetToken.setUsedAt(LocalDateTime.now());
		passwordResetTokenRepository.save(resetToken);

		return true;
	}

	private void sendPasswordResetEmail(String to, String token) {
		String subject = "StreetAsk - Password reset";
		String plainText = "Hello,\n\nWe received a request to reset your password.\n"
				+ "Copy and paste this token in the app to continue:\n" + token + "\n\n"
				+ "If you did not request this change, ignore this message.\n\n"
				+ "This token expires in " + PASSWORD_RESET_TOKEN_MINUTES + " minutes.";

		String html = """
				<html>
				  <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,sans-serif;color:#1f2937;">
				    <div style="max-width:560px;margin:24px auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;">
				      <div style="background:#dc2626;color:#ffffff;padding:16px 20px;font-size:18px;font-weight:700;">
				        StreetAsk · Password reset
				      </div>
				      <div style="padding:20px;line-height:1.5;font-size:14px;">
				        <p style="margin:0 0 12px;">Hello,</p>
				        <p style="margin:0 0 14px;">We received a request to reset your password.</p>
				        <p style="margin:0 0 8px;"><strong>Copy and paste this token in the app:</strong></p>
				        <div style="background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:12px;text-align:center;">
				          <span style="font-family:Consolas,Monaco,monospace;font-size:18px;letter-spacing:1px;font-weight:700;color:#111827;">%s</span>
				        </div>
				        <p style="margin:14px 0 0;">This token expires in <strong>%d minutes</strong>.</p>
				        <p style="margin:10px 0 0;color:#6b7280;">If you did not request this change, ignore this message.</p>
				      </div>
				    </div>
				  </body>
				</html>
				"""
				.formatted(token, PASSWORD_RESET_TOKEN_MINUTES);

		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
			helper.setFrom(mailFrom);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(html, true);
			mailSender.send(mimeMessage);
		} catch (Exception exception) {
			SimpleMailMessage fallbackMessage = new SimpleMailMessage();
			fallbackMessage.setFrom(mailFrom);
			fallbackMessage.setTo(to);
			fallbackMessage.setSubject(subject);
			fallbackMessage.setText(plainText);
			mailSender.send(fallbackMessage);
		}
	}

	@Transactional
	public void createRegularUser(@Valid CompleteSignupRequest request) {
		// Find the basic user created in the first step
		User basicUser = userService.findUser(request.getEmail());

		RegularUser user = new RegularUser();

		// Copy base User fields
		user.setEmail(basicUser.getEmail());
		user.setUserName(basicUser.getUserName());
		user.setPassword(basicUser.getPassword());
		user.setFirstName(basicUser.getFirstName());
		user.setLastName(basicUser.getLastName());
		user.setAccountType(AccountType.REGULAR_USER);
		user.setActive(true);
		user.setCreatedAt(basicUser.getCreatedAt());

		// Regular user defaults
		user.setCoinBalance(0);
		user.setRating(0.0f);
		user.setTotalLikesReceived(0);
		user.setTotalDislikesReceived(0);
		user.setVerified(false);
		user.setVisibilityRadiusKm(DEFAULT_VISIBILITY_RADIUS_KM);
		user.setPremiumActive(false);

		// Assign USER authority
		Authorities role = authoritiesService.findByAuthority("USER");
		user.setAuthority(role);

		// Delete basic user and flush to force DELETE
		userService.deleteUser(basicUser.getId());
		entityManager.flush();

		regularUserRepository.save(user);
	}

	@Transactional
	public User createBasicUser(@Valid SignupRequest request) {
		// Create temporary basic user; then convert to RegularUser or BusinessAccount
		User user = new User();
		user.setEmail(request.getEmail());
		user.setUserName(request.getUserName());
		user.setPassword(encoder.encode(request.getPassword()));
		user.setFirstName(request.getFirstName());
		user.setLastName(request.getLastName());
		user.setActive(false);
		user.setCreatedAt(LocalDateTime.now());

		// Assign temporary USER authority
		Authorities role = authoritiesService.findByAuthority("USER");
		user.setAuthority(role);

		return userService.saveUser(user);
	}

	@Transactional
	public void convertToBusinessUser(@Valid BusinessSignupRequest request) {
		// Find the basic user created in the first step
		User basicUser = userService.findUser(request.getEmail());

		BusinessAccount businessAccount = new BusinessAccount();

		businessAccount.setEmail(basicUser.getEmail());
		businessAccount.setUserName(basicUser.getUserName());
		businessAccount.setPassword(basicUser.getPassword());
		businessAccount.setFirstName(basicUser.getFirstName());
		businessAccount.setLastName(basicUser.getLastName());
		businessAccount.setAccountType(AccountType.BUSINESS);
		businessAccount.setActive(false);
		businessAccount.setCreatedAt(basicUser.getCreatedAt());

		businessAccount.setTaxId(request.getTaxId());
		businessAccount.setCompanyName(request.getCompanyName());
		businessAccount.setAddress(request.getAddress());
		businessAccount.setWebsite(request.getWebsite());
		businessAccount.setDescription(request.getDescription());

		businessAccount.setVerified(false);
		businessAccount.setRating(0.0f);
		businessAccount.setRequestStatus(RequestStatus.PENDING);
		businessAccount.setSubscriptionActive(false);

		// Assign BUSINESS authority
		Authorities role = authoritiesService.findByAuthority("BUSINESS");
		businessAccount.setAuthority(role);

		// Delete basic user and flush to force DELETE
		userService.deleteUser(basicUser.getId());
		entityManager.flush();

		businessAccountRepository.save(businessAccount);
	}

}
