package com.smarthr.controller.employee;

import com.smarthr.dto.PasswordForm;
import com.smarthr.dto.ProfileForm;
import com.smarthr.entity.Employee;
import com.smarthr.exception.BusinessException;
import com.smarthr.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
public class EmployeeProfileController {

    private final EmployeeService employees;

    public EmployeeProfileController(EmployeeService employees) {
        this.employees = employees;
    }

    @GetMapping("/employee/profile")
    public String profile(Principal principal, Model model) {
        Employee e = employees.getByUsername(principal.getName());
        return show(model, e, ProfileForm.from(e), new PasswordForm());
    }

    @PostMapping("/employee/profile")
    public String update(@Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult br, Principal principal,
                         Model model, RedirectAttributes ra) {
        Employee e = employees.getByUsername(principal.getName());
        if (br.hasErrors()) return show(model, e, form, new PasswordForm());
        employees.updateProfile(e, form);
        ra.addFlashAttribute("success", "Your profile was updated.");
        return "redirect:/employee/profile";
    }

    @PostMapping("/employee/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordForm form, BindingResult br, Principal principal,
                                 Model model, RedirectAttributes ra) {
        Employee e = employees.getByUsername(principal.getName());
        if (!br.hasErrors()) {
            try {
                employees.changePassword(principal.getName(), form);
                ra.addFlashAttribute("success", "Your password was changed.");
                return "redirect:/employee/profile";
            } catch (BusinessException ex) {
                model.addAttribute("passwordError", ex.getMessage());
            }
        }
        return show(model, e, ProfileForm.from(e), form);
    }

    private String show(Model model, Employee e, ProfileForm profile, PasswordForm password) {
        model.addAttribute("employee", e);
        model.addAttribute("profileForm", profile);
        model.addAttribute("passwordForm", password);
        return "employee/profile";
    }
}
