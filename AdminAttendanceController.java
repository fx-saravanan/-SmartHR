package com.smarthr.controller.admin;

import com.smarthr.dto.AttendanceRow;
import com.smarthr.service.AttendanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class AdminAttendanceController {

    private final AttendanceService attendance;

    public AdminAttendanceController(AttendanceService attendance) {
        this.attendance = attendance;
    }

    @GetMapping("/admin/attendance")
    public String board(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<AttendanceRow> rows = attendance.board(day);
        model.addAttribute("day", day);
        model.addAttribute("rows", rows);
        model.addAttribute("prev", day.minusDays(1));
        model.addAttribute("next", day.plusDays(1));
        model.addAttribute("present", rows.stream().filter(r -> r.getStatus().equals("PRESENT")).count());
        model.addAttribute("late", rows.stream().filter(r -> r.getStatus().equals("LATE")).count());
        model.addAttribute("halfDay", rows.stream().filter(r -> r.getStatus().equals("HALF_DAY")).count());
        model.addAttribute("onLeave", rows.stream().filter(r -> r.getStatus().equals("ON_LEAVE")).count());
        model.addAttribute("absent", rows.stream().filter(r -> r.getStatus().equals("ABSENT")).count());
        return "admin/attendance";
    }
}
