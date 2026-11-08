package com.smarthr.controller.employee;

import com.smarthr.entity.Attendance;
import com.smarthr.entity.Employee;
import com.smarthr.exception.BusinessException;
import com.smarthr.service.AttendanceService;
import com.smarthr.service.EmployeeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/employee/attendance")
public class EmployeeAttendanceController {

    private final EmployeeService employees;
    private final AttendanceService attendance;

    public EmployeeAttendanceController(EmployeeService employees, AttendanceService attendance) {
        this.employees = employees;
        this.attendance = attendance;
    }

    @GetMapping
    public String view(@RequestParam(required = false) String month, Principal principal, Model model) {
        Employee e = employees.getByUsername(principal.getName());
        YearMonth ym;
        try {
            ym = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        } catch (DateTimeException ex) {
            ym = YearMonth.now();
        }
        List<Attendance> records = attendance.forMonth(e, ym);
        model.addAttribute("records", records);
        model.addAttribute("summary", attendance.summarise(records));
        model.addAttribute("today", attendance.today(e).orElse(null));
        model.addAttribute("month", ym.toString());
        model.addAttribute("monthLabel", ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + ym.getYear());
        model.addAttribute("prevMonth", ym.minusMonths(1).toString());
        model.addAttribute("nextMonth", ym.plusMonths(1).toString());
        model.addAttribute("canGoNext", ym.isBefore(YearMonth.now()));
        return "employee/attendance";
    }

    @PostMapping("/check-in")
    public String checkIn(Principal principal, RedirectAttributes ra) {
        try {
            Attendance a = attendance.checkIn(employees.getByUsername(principal.getName()));
            ra.addFlashAttribute("success", "Checked in at " + a.getCheckIn() + (a.getStatus().name().equals("LATE") ? " (marked late)." : "."));
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/employee/attendance";
    }

    @PostMapping("/check-out")
    public String checkOut(Principal principal, RedirectAttributes ra) {
        try {
            Attendance a = attendance.checkOut(employees.getByUsername(principal.getName()));
            ra.addFlashAttribute("success", "Checked out at " + a.getCheckOut() + ". You worked " + a.getWorkedLabel() + " today.");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/employee/attendance";
    }
}
