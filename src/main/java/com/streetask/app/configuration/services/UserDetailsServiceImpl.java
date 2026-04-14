package com.streetask.app.configuration.services;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import com.streetask.app.user.User;
import com.streetask.app.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	@Autowired
	UserRepository userRepository;

	@Override
	@Transactional
	public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
		String normalizedIdentifier = identifier == null ? "" : identifier.trim();
		Optional<User> userByIdentifier = userRepository.findByEmailIgnoreCase(normalizedIdentifier)
				.or(() -> userRepository.findByUserNameIgnoreCase(normalizedIdentifier))
				.or(() -> findByIdIfUuid(normalizedIdentifier));
		User user = userByIdentifier
				.orElseThrow(() -> new UsernameNotFoundException(
						"User Not Found with email, username, or id: " + normalizedIdentifier));

		return UserDetailsImpl.build(user);
	}

	private Optional<User> findByIdIfUuid(String identifier) {
		try {
			return userRepository.findById(UUID.fromString(identifier));
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

}
