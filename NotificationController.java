package com.smarthr.controller;

import com.smarthr.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;

@Controller
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping("/notifications")
    public String list(Principal principal, Model model) {
        model.addAttribute("items", notifications.latestFor(principal.getName()));
        return "notifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAll(Principal principal) {
        notifications.markAllRead(principal.getName());
        return "redirect:/notifications";
    }
}
