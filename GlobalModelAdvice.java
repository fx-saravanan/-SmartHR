package com.smarthr.config;

import com.smarthr.service.NotificationService;
import com.smarthr.repository.EmployeeRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds the values the shared layout needs (who is logged in, unread notifications) to every view. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final NotificationService notifications;
    private final EmployeeRepository employees;

    public GlobalModelAdvice(NotificationService notifications, EmployeeRepository employees) {
        this.notifications = notifications;
        this.employees = employees;
    }

    @ModelAttribute
    public void common(Authentication auth, Model model) {
        boolean loggedIn = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        model.addAttribute("loggedIn", loggedIn);
        if (!loggedIn) return;
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", admin);
        model.addAttribute("unreadCount", notifications.unreadCount(auth.getName()));
        String display = admin ? "Administrator"
                : employees.findByUserUsernameIgnoreCase(auth.getName()).map(e -> e.getFullName()).orElse(auth.getName());
        model.addAttribute("currentName", display);
    }
}
