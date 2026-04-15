package com.streetask.app.functionalities.notifications.model;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.streetask.app.user.User;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    void deleteByUser(User user);
}
