package com.streetask.app.functionalities.notifications.model;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.streetask.app.user.RegularUser;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    void deleteByUser(RegularUser user);
}
