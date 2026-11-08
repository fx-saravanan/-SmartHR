package com.smarthr.controller.employee;

import com.smarthr.entity.Employee;
import com.smarthr.entity.LeaveStatus;
import com.smarthr.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;

@Controller
public class EmployeeDashboardController {

    private final EmployeeService employees;
    private final LeaveBalanceService balances;
    private final LeaveService leaves;
    private final AttendanceService attendance;
    private final PayrollService payroll;
    private final NotificationService notifications;

    public EmployeeDashboardController(EmployeeService employees, LeaveBalanceService balances, LeaveService leaves,
                                       AttendanceService attendance, PayrollService payroll, NotificationService notifications) {
        this.employees = employees;
        this.balances = balances;
        this.leaves = leaves;
        this.attendance = attendance;
        this.payroll = payroll;
        this.notifications = notifications;
    }

    @GetMapping("/employee/dashboard")
    public String dashboard(Principal principal, Model model) {
        Employee e = employees.getByUsername(principal.getName());
        model.addAttribute("employee", e);
        model.addAttribute("balances", balances.viewsFor(e, LocalDate.now().getYear()));
        model.addAttribute("recentLeaves", leaves.forEmployee(e).stream().limit(5).toList());
        model.addAttribute("pendingCount", leaves.forEmployee(e).stream().filter(l -> l.getStatus() == LeaveStatus.PENDING).count());
        model.addAttribute("today", attendance.today(e).orElse(null));
        model.addAttribute("summary", attendance.summarise(attendance.forMonth(e, YearMonth.now())));
        model.addAttribute("latestSlip", payroll.forEmployee(e).stream().findFirst().orElse(null));
        model.addAttribute("recentNotes", notifications.recentFor(principal.getName()));
        return "employee/dashboard";
    }
}
