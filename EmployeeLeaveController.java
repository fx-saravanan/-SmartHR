package com.smarthr.controller.employee;

import com.smarthr.dto.LeaveForm;
import com.smarthr.entity.Employee;
import com.smarthr.entity.LeaveRequest;
import com.smarthr.entity.LeaveType;
import com.smarthr.exception.BusinessException;
import com.smarthr.service.EmployeeService;
import com.smarthr.service.LeaveBalanceService;
import com.smarthr.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/employee/leaves")
public class EmployeeLeaveController {

    private final EmployeeService employees;
    private final LeaveService leaves;
    private final LeaveBalanceService balances;

    public EmployeeLeaveController(EmployeeService employees, LeaveService leaves, LeaveBalanceService balances) {
        this.employees = employees;
        this.leaves = leaves;
        this.balances = balances;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        Employee e = employees.getByUsername(principal.getName());
        model.addAttribute("balances", balances.viewsFor(e, LocalDate.now().getYear()));
        model.addAttribute("requests", leaves.forEmployee(e));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("year", LocalDate.now().getYear());
        return "employee/leaves";
    }

    @GetMapping("/apply")
    public String applyForm(Principal principal, Model model) {
        LeaveForm form = new LeaveForm();
        form.setLeaveType(LeaveType.CASUAL);
        return showForm(model, principal, form);
    }

    @PostMapping("/apply")
    public String apply(@Valid @ModelAttribute("form") LeaveForm form, BindingResult br, Principal principal, Model model,
                        RedirectAttributes ra) {
        if (!br.hasErrors()) {
            try {
                LeaveRequest r = leaves.apply(employees.getByUsername(principal.getName()), form);
                ra.addFlashAttribute("success", "Request sent: " + r.getDays() + " working day(s) of " + r.getLeaveType().getLabel().toLowerCase()
                        + ". You will be notified when it is decided.");
                return "redirect:/employee/leaves";
            } catch (BusinessException ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }
        return showForm(model, principal, form);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            leaves.cancel(id, employees.getByUsername(principal.getName()));
            ra.addFlashAttribute("success", "Leave request cancelled.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/employee/leaves";
    }

    private String showForm(Model model, Principal principal, LeaveForm form) {
        Employee e = employees.getByUsername(principal.getName());
        model.addAttribute("form", form);
        model.addAttribute("leaveTypes", LeaveType.values());
        model.addAttribute("balances", balances.viewsFor(e, LocalDate.now().getYear()));
        model.addAttribute("today", LocalDate.now());
        return "employee/leave-apply";
    }
}
