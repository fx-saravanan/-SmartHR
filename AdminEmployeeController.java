package com.smarthr.controller.admin;

import com.smarthr.dto.EmployeeForm;
import com.smarthr.entity.Employee;
import com.smarthr.exception.BusinessException;
import com.smarthr.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;

@Controller
@RequestMapping("/admin/employees")
public class AdminEmployeeController {

    private final EmployeeService employees;
    private final DepartmentService departments;
    private final LeaveBalanceService balances;
    private final LeaveService leaves;
    private final AttendanceService attendance;
    private final PayrollService payroll;

    public AdminEmployeeController(EmployeeService employees, DepartmentService departments, LeaveBalanceService balances,
                                   LeaveService leaves, AttendanceService attendance, PayrollService payroll) {
        this.employees = employees;
        this.departments = departments;
        this.balances = balances;
        this.leaves = leaves;
        this.attendance = attendance;
        this.payroll = payroll;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") Long dept, Model model) {
        model.addAttribute("employees", employees.search(q, dept));
        model.addAttribute("departments", departments.findAll());
        model.addAttribute("q", q);
        model.addAttribute("dept", dept);
        return "admin/employees";
    }

    @GetMapping("/new")
    public String create(Model model) {
        EmployeeForm form = new EmployeeForm();
        form.setJoiningDate(LocalDate.now());
        model.addAttribute("form", form);
        model.addAttribute("departments", departments.findAll());
        return "admin/employee-form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("form", EmployeeForm.from(employees.get(id)));
        model.addAttribute("departments", departments.findAll());
        return "admin/employee-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") EmployeeForm form, BindingResult br, Model model, RedirectAttributes ra) {
        boolean creating = form.getId() == null;
        if (creating && (form.getPassword() == null || form.getPassword().length() < 8)) {
            br.rejectValue("password", "required", "Set an initial password of at least 8 characters");
        }
        if (!br.hasErrors()) {
            try {
                Employee saved = creating ? employees.create(form) : employees.update(form);
                ra.addFlashAttribute("success", saved.getFullName() + (creating ? " was added." : " was updated."));
                return "redirect:/admin/employees/" + saved.getId();
            } catch (BusinessException ex) {
                br.rejectValue("email", "business", ex.getMessage());
            }
        }
        model.addAttribute("departments", departments.findAll());
        return "admin/employee-form";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Employee e = employees.get(id);
        int year = LocalDate.now().getYear();
        model.addAttribute("employee", e);
        model.addAttribute("balances", balances.viewsFor(e, year));
        model.addAttribute("year", year);
        model.addAttribute("leaves", leaves.forEmployee(e).stream().limit(8).toList());
        model.addAttribute("attendance", attendance.forMonth(e, YearMonth.now()).stream().limit(10).toList());
        model.addAttribute("payslips", payroll.forEmployee(e).stream().limit(6).toList());
        return "admin/employee-view";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        Employee e = employees.get(id);
        employees.setActive(id, !e.isActive());
        ra.addFlashAttribute("success", e.getFullName() + (e.isActive() ? " was reactivated and can sign in." : " was deactivated and can no longer sign in."));
        return "redirect:/admin/employees/" + id;
    }
}
