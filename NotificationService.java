package com.smarthr.service;

import com.smarthr.entity.AppUser;
import com.smarthr.entity.Notification;
import com.smarthr.entity.NotificationType;
import com.smarthr.entity.Role;
import com.smarthr.repository.AppUserRepository;
import com.smarthr.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notifications;
    private final AppUserRepository users;

    public NotificationService(NotificationRepository notifications, AppUserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    public void notify(AppUser recipient, NotificationType type, String message) {
        if (recipient != null) notifications.save(new Notification(recipient, type, message));
    }

    public void notifyAdmins(NotificationType type, String message) {
        for (AppUser admin : users.findByRole(Role.ADMIN)) notify(admin, type, message);
    }

    @Transactional(readOnly = true)
    public List<Notification> latestFor(String username) {
        return users.findByUsernameIgnoreCase(username)
                .map(u -> notifications.findTop50ByRecipientIdOrderByCreatedAtDesc(u.getId())).orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<Notification> recentFor(String username) {
        return users.findByUsernameIgnoreCase(username)
                .map(u -> notifications.findTop5ByRecipientIdOrderByCreatedAtDesc(u.getId())).orElse(List.of());
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        return notifications.countByRecipientUsernameIgnoreCaseAndReadFlagFalse(username);
    }

    public void markAllRead(String username) {
        users.findByUsernameIgnoreCase(username).ifPresent(u -> notifications.markAllRead(u.getId()));
    }
}
