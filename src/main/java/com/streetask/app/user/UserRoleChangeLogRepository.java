package com.streetask.app.user;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleChangeLogRepository extends JpaRepository<UserRoleChangeLog, UUID> {

    List<UserRoleChangeLog> findByUserIdOrderByChangedAtDesc(UUID userId);

    List<UserRoleChangeLog> findAllByOrderByChangedAtDesc();
}
