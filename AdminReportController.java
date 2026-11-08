package com.smarthr.controller.admin;

import com.smarthr.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

    private final ReportService reports;

    public AdminReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping
    public String page(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("from", today.withDayOfYear(1));
        model.addAttribute("to", today);
        model.addAttribute("year", today.getYear());
        model.addAttribute("month", today.getMonthValue());
        return "admin/reports";
    }

    @GetMapping("/employees.csv")
    public ResponseEntity<byte[]> employees() { return csv("employees.csv", reports.employeeReport()); }

    @GetMapping("/leaves.csv")
    public ResponseEntity<byte[]> leaves(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return csv("leaves-" + from + "-to-" + to + ".csv", reports.leaveReport(from, to));
    }

    @GetMapping("/attendance.csv")
    public ResponseEntity<byte[]> attendance(@RequestParam int year, @RequestParam int month) {
        YearMonth ym = YearMonth.of(year, month);
        return csv("attendance-" + ym + ".csv", reports.attendanceReport(ym));
    }

    @GetMapping("/payroll.csv")
    public ResponseEntity<byte[]> payroll(@RequestParam int year, @RequestParam int month) {
        YearMonth ym = YearMonth.of(year, month);
        return csv("payroll-" + ym + ".csv", reports.payrollReport(ym.getYear(), ym.getMonthValue()));
    }

    private ResponseEntity<byte[]> csv(String filename, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body.getBytes(StandardCharsets.UTF_8));
    }
}
