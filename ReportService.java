package com.smarthr.service;

import com.smarthr.entity.*;
import com.smarthr.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Builds CSV reports (opens directly in Excel). */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final EmployeeRepository employees;
    private final AttendanceService attendance;
    private final PayrollService payroll;
    private final com.smarthr.repository.LeaveRequestRepository leaves;

    public ReportService(EmployeeRepository employees, AttendanceService attendance, PayrollService payroll,
                         com.smarthr.repository.LeaveRequestRepository leaves) {
        this.employees = employees;
        this.attendance = attendance;
        this.payroll = payroll;
        this.leaves = leaves;
    }

    public String employeeReport() {
        List<List<String>> rows = new ArrayList<>();
        for (Employee e : employees.search("", 0L)) {
            rows.add(List.of(e.getEmployeeCode(), e.getFullName(), e.getEmail(), nz(e.getPhone()),
                    e.getDepartment() == null ? "" : e.getDepartment().getName(), nz(e.getDesignation()),
                    String.valueOf(e.getJoiningDate()), e.getBasicSalary().toPlainString(), e.getHra().toPlainString(),
                    e.getAllowances().toPlainString(), e.isActive() ? "Active" : "Inactive"));
        }
        return csv(List.of("Employee ID", "Name", "Email", "Phone", "Department", "Designation", "Joined", "Basic", "HRA",
                "Allowances", "Status"), rows);
    }

    public String leaveReport(LocalDate from, LocalDate to) {
        List<List<String>> rows = new ArrayList<>();
        for (LeaveRequest l : leaves.findBetween(from, to)) {
            Employee e = l.getEmployee();
            rows.add(List.of(e.getEmployeeCode(), e.getFullName(), e.getDepartment() == null ? "" : e.getDepartment().getName(),
                    l.getLeaveType().getLabel(), l.getStartDate().toString(), l.getEndDate().toString(), String.valueOf(l.getDays()),
                    l.getStatus().name(), nz(l.getReason()), nz(l.getAdminRemark())));
        }
        return csv(List.of("Employee ID", "Name", "Department", "Leave type", "From", "To", "Working days", "Status", "Reason",
                "Admin remark"), rows);
    }

    public String attendanceReport(YearMonth ym) {
        List<List<String>> rows = attendance.between(ym.atDay(1), ym.atEndOfMonth()).stream().map(a -> List.of(
                a.getEmployee().getEmployeeCode(), a.getEmployee().getFullName(), a.getWorkDate().toString(),
                String.valueOf(a.getCheckIn()), a.getCheckOut() == null ? "" : a.getCheckOut().toString(),
                a.getWorkedLabel(), a.getStatus().name())).collect(Collectors.toList());
        return csv(List.of("Employee ID", "Name", "Date", "Check in", "Check out", "Worked", "Status"), rows);
    }

    public String payrollReport(int year, int month) {
        List<List<String>> rows = payroll.forPeriod(year, month).stream().map(p -> List.of(
                p.getEmployee().getEmployeeCode(), p.getEmployee().getFullName(), p.getPeriodLabel(),
                String.valueOf(p.getWorkingDays()), String.valueOf(p.getUnpaidLeaveDays()),
                p.getBasic().toPlainString(), p.getHra().toPlainString(), p.getAllowances().toPlainString(),
                p.getGrossPay().toPlainString(), p.getPfDeduction().toPlainString(), p.getTaxDeduction().toPlainString(),
                p.getUnpaidDeduction().toPlainString(), p.getNetPay().toPlainString(), p.getStatus().name())).collect(Collectors.toList());
        return csv(List.of("Employee ID", "Name", "Period", "Working days", "Unpaid days", "Basic", "HRA", "Allowances", "Gross",
                "PF", "Tax", "Loss of pay", "Net pay", "Status"), rows);
    }

    private static String csv(List<String> header, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append(String.join(",", header.stream().map(ReportService::cell).toList())).append("\r\n");
        for (List<String> r : rows) sb.append(String.join(",", r.stream().map(ReportService::cell).toList())).append("\r\n");
        return sb.toString();
    }

    /** Quotes the value and neutralises spreadsheet formulas (=, +, -, @ at the start). */
    private static String cell(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
