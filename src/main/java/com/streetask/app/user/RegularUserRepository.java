package com.streetask.app.user;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.CrudRepository;

import jakarta.persistence.LockModeType;

public interface RegularUserRepository extends CrudRepository<RegularUser, UUID> {

    Optional<RegularUser> findByEmail(String email);

    Optional<RegularUser> findByUserNameIgnoreCase(String userName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from RegularUser u where u.id = :id")
    Optional<RegularUser> findByIdForUpdate(@Param("id") UUID id);
}
