package com.smarthr.controller;

import com.smarthr.dto.RegisterForm;
import com.smarthr.exception.BusinessException;
import com.smarthr.service.DepartmentService;
import com.smarthr.service.EmployeeService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final EmployeeService employees;
    private final DepartmentService departments;

    public AuthController(EmployeeService employees, DepartmentService departments) {
        this.employees = employees;
        this.departments = departments;
    }

    /** Public landing page; signed-in users go straight to their dashboard. */
    @GetMapping("/")
    public String root(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) return "redirect:/default";
        return "index";
    }

    /** Sends each role to its own dashboard after login. */
    @GetMapping("/default")
    public String landing(Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return admin ? "redirect:/admin/dashboard" : "redirect:/employee/dashboard";
    }

    @GetMapping("/login")
    public String login(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) return "redirect:/default";
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        model.addAttribute("departments", departments.findAll());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult br, Model model, RedirectAttributes ra) {
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            br.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (!br.hasErrors()) {
            try {
                employees.register(form);
                ra.addFlashAttribute("success", "Your account is ready. Sign in with " + form.getEmail().trim().toLowerCase() + ".");
                return "redirect:/login";
            } catch (BusinessException ex) {
                br.rejectValue("email", "duplicate", ex.getMessage());
            }
        }
        model.addAttribute("departments", departments.findAll());
        return "register";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("status", 403);
        model.addAttribute("error", "Forbidden");
        model.addAttribute("message", "Your account does not have access to that page.");
        return "error";
    }
}
