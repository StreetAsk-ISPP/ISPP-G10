package com.streetask.app.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, UUID> {

	// @Modifying
	// @Query("DELETE FROM Owner o WHERE o.user.email = :email")
	// void deleteOwnerOfUser(String email);
	//
	// @Modifying
	// @Query("DELETE FROM Pet p WHERE p.owner.id = :id")
	// public void deletePetsOfOwner(@Param("id") int id);

	Optional<User> findByEmail(String email);

	Optional<User> findByEmailIgnoreCase(String email);

	Optional<User> findByUserNameIgnoreCase(String userName);

	Boolean existsByEmail(String email);

	Boolean existsByUserName(String userName);

	Optional<User> findById(UUID id);

	@Query("SELECT u FROM User u WHERE u.authority.authority = :auth")
	Iterable<User> findAllByAuthority(String auth);

	@Query("SELECT u FROM User u WHERE u.accountType IS NULL AND u.active = false AND u.authority.authority = 'USER' AND u.createdAt < :cutoff")
	List<User> findStalePendingBasicUsers(LocalDateTime cutoff);

}
