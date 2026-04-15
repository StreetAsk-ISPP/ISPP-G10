package com.streetask.app.configuration.jwt;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import com.streetask.app.configuration.services.UserDetailsServiceImpl;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthTokenFilter extends OncePerRequestFilter {

	@Autowired
	private JwtUtils jwtUtils;

	@Autowired
	private UserDetailsServiceImpl userDetailsService;

	@Autowired(required = false)
	private UserRepository userRepository;

	private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			String jwt = parseJwt(request);
			if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
				String username = jwtUtils.getUserNameFromJwtToken(jwt);
				Long tokenVersionFromJwt = jwtUtils.getTokenVersionFromJwtToken(jwt);

				// Validate token version against database to detect role changes
				if (userRepository != null && !isTokenVersionValid(username, tokenVersionFromJwt)) {
					logger.warn("Token version mismatch for user {}. Token invalidated due to role change.", username);
					filterChain.doFilter(request, response);
					return;
				}

				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (Exception e) {
			logger.error("Cannot set user authentication: {}", e);
		}

		filterChain.doFilter(request, response);
	}

	private boolean isTokenVersionValid(String username, Long tokenVersionFromJwt) {
		try {
			User user = userRepository.findByEmailIgnoreCase(username)
					.or(() -> userRepository.findByUserNameIgnoreCase(username))
					.or(() -> findByUuid(username))
					.orElse(null);
			if (user == null) {
				return false;
			}
			Long currentTokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
			return currentTokenVersion.equals(tokenVersionFromJwt);
		} catch (Exception e) {
			logger.error("Error validating token version: {}", e.getMessage());
			return false;
		}
	}

	private java.util.Optional<User> findByUuid(String identifier) {
		try {
			return userRepository.findById(UUID.fromString(identifier));
		} catch (IllegalArgumentException ex) {
			return java.util.Optional.empty();
		}
	}

	private String parseJwt(HttpServletRequest request) {
		String headerAuth = request.getHeader("Authorization");

		if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
			return headerAuth.substring(7, headerAuth.length());
		}

		return null;
	}

}
