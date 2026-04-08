package com.streetask.app.moderation;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.streetask.app.model.Strike;
import com.streetask.app.user.RegularUser;

public interface StrikeRepository extends JpaRepository<Strike, UUID> {
    List<Strike> findByUserOrderByIssuedAtDesc(RegularUser user);
    long countByUser(RegularUser user);
}
