package com.streetask.app.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import com.streetask.app.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthoritiesService {

	private AuthoritiesRepository authoritiesRepository;

	@Autowired
	public AuthoritiesService(AuthoritiesRepository authoritiesRepository) {
		this.authoritiesRepository = authoritiesRepository;
	}

	@Transactional(readOnly = true)
	public Iterable<Authorities> findAll() {
		return this.authoritiesRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Authorities findByAuthority(String authority) {
		return this.authoritiesRepository.findByName(authority)
				.orElseThrow(() -> new ResourceNotFoundException("Authority", "Name", authority));
	}

	@Transactional
	public void saveAuthorities(Authorities authorities) throws DataAccessException {
		authoritiesRepository.save(authorities);
	}

}



