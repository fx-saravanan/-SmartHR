package com.smarthr.service;

import com.smarthr.dto.EmployeeRisk;
import com.smarthr.entity.*;
import com.smarthr.repository.DepartmentRepository;
import com.smarthr.repository.EmployeeRepository;
import com.smarthr.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/** Aggregations behind the admin dashboard and the analytics charts. */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final LeaveRequestRepository leaves;
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final AttendanceService attendance;

    public AnalyticsService(LeaveRequestRepository leaves, EmployeeRepository employees, DepartmentRepository departments,
                            AttendanceService attendance) {
        this.leaves = leaves;
        this.employees = employees;
        this.departments = departments;
        this.attendance = attendance;
    }

    public static List<String> monthLabels() {
        List<String> labels = new ArrayList<>();
        for (Month m : Month.values()) labels.add(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        return labels;
    }

    private List<LeaveRequest> approvedInYear(int year) {
        return leaves.findSince(LeaveStatus.APPROVED, LocalDate.of(year, 1, 1)).stream()
                .filter(l -> l.getStartDate().getYear() == year).toList();
    }

    /** Approved leave days by month of the start date, 12 entries. */
    public List<Integer> monthlyLeaveDays(int year) {
        int[] totals = new int[12];
        for (LeaveRequest l : approvedInYear(year)) totals[l.getStartDate().getMonthValue() - 1] += l.getDays();
        return Arrays.stream(totals).boxed().toList();
    }

    public Map<String, Integer> leaveDaysByDepartment(int year) {
        Map<String, Integer> map = new TreeMap<>();
        for (Department d : departments.findAll()) map.put(d.getName(), 0);
        for (LeaveRequest l : approvedInYear(year)) {
            Department d = l.getEmployee().getDepartment();
            map.merge(d == null ? "Unassigned" : d.getName(), l.getDays(), Integer::sum);
        }
        return map;
    }

    public Map<String, Integer> leaveDaysByType(int year) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (LeaveType t : LeaveType.values()) map.put(t.getLabel(), 0);
        for (LeaveRequest l : approvedInYear(year)) map.merge(l.getLeaveType().getLabel(), l.getDays(), Integer::sum);
        return map;
    }

    /** Headcount per department (active employees). */
    public Map<String, Long> headcountByDepartment() {
        Map<String, Long> map = new TreeMap<>();
        for (Employee e : employees.findByActiveTrue()) {
            map.merge(e.getDepartment() == null ? "Unassigned" : e.getDepartment().getName(), 1L, Long::sum);
        }
        return map;
    }

    /** Employees whose last-six-month leave is at least 1.5x the average of active employees (and at least 6 days). */
    public List<EmployeeRisk> highLeaveEmployees() {
        LocalDate from = LocalDate.now().minusMonths(6);
        Map<Long, int[]> perEmployee = new HashMap<>();
        Map<Long, Employee> byId = new HashMap<>();
        for (LeaveRequest l : leaves.findSince(LeaveStatus.APPROVED, from)) {
            if (l.getStartDate().isAfter(LocalDate.now())) continue;
            Employee e = l.getEmployee();
            if (!e.isActive()) continue;
            byId.put(e.getId(), e);
            int[] a = perEmployee.computeIfAbsent(e.getId(), k -> new int[2]);
            a[0] += l.getDays();
            if (l.getLeaveType() == LeaveType.SICK) a[1] += l.getDays();
        }
        long active = Math.max(1, employees.countByActiveTrue());
        double avg = perEmployee.values().stream().mapToInt(a -> a[0]).sum() / (double) active;
        if (avg <= 0) return List.of();
        return perEmployee.entrySet().stream()
                .filter(en -> en.getValue()[0] >= 6 && en.getValue()[0] >= avg * 1.5)
                .map(en -> {
                    Employee e = byId.get(en.getKey());
                    return new EmployeeRisk(e.getFullName(), e.getDepartment() == null ? "-" : e.getDepartment().getName(),
                            en.getValue()[0], en.getValue()[1], Math.round(en.getValue()[0] / avg * 10.0) / 10.0);
                })
                .sorted(Comparator.comparingDouble(EmployeeRisk::getRatioToAverage).reversed())
                .limit(8)
                .collect(Collectors.toList());
    }

    /** Numbers for the admin dashboard tiles. */
    public Map<String, Object> dashboardStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        m.put("totalEmployees", employees.count());
        m.put("activeEmployees", employees.countByActiveTrue());
        m.put("departmentCount", departments.count());
        m.put("presentToday", attendance.presentToday());
        m.put("onLeaveToday", leaves.findCovering(LeaveStatus.APPROVED, today).stream().map(l -> l.getEmployee().getId()).distinct().count());
        m.put("pendingLeaves", leaves.countByStatus(LeaveStatus.PENDING));
        return m;
    }
}
