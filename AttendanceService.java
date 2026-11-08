package com.smarthr.service;

import com.smarthr.dto.AttendanceRow;
import com.smarthr.entity.*;
import com.smarthr.exception.BusinessException;
import com.smarthr.repository.AttendanceRepository;
import com.smarthr.repository.EmployeeRepository;
import com.smarthr.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {

    /** Worked time below this is recorded as a half day. */
    private static final long HALF_DAY_MINUTES = 4 * 60;

    private final AttendanceRepository attendance;
    private final EmployeeRepository employees;
    private final LeaveRequestRepository leaves;
    private final LocalTime lateAfter;

    public AttendanceService(AttendanceRepository attendance, EmployeeRepository employees, LeaveRequestRepository leaves,
                             @Value("${smarthr.attendance.late-after:09:45}") String lateAfter) {
        this.attendance = attendance;
        this.employees = employees;
        this.leaves = leaves;
        this.lateAfter = LocalTime.parse(lateAfter);
    }

    public Attendance checkIn(Employee e) {
        LocalDate today = LocalDate.now();
        if (attendance.findByEmployeeIdAndWorkDate(e.getId(), today).isPresent()) {
            throw new BusinessException("You have already checked in today.");
        }
        if (leaves.findCovering(LeaveStatus.APPROVED, today).stream().anyMatch(l -> l.getEmployee().getId().equals(e.getId()))) {
            throw new BusinessException("You are on approved leave today, so check-in is not needed.");
        }
        LocalTime now = LocalTime.now().withNano(0);
        Attendance a = new Attendance();
        a.setEmployee(e);
        a.setWorkDate(today);
        a.setCheckIn(now);
        a.setStatus(now.isAfter(lateAfter) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
        return attendance.save(a);
    }

    public Attendance checkOut(Employee e) {
        Attendance a = attendance.findByEmployeeIdAndWorkDate(e.getId(), LocalDate.now())
                .orElseThrow(() -> new BusinessException("Check in first, then check out."));
        if (a.getCheckOut() != null) throw new BusinessException("You have already checked out today.");
        a.setCheckOut(LocalTime.now().withNano(0));
        if (a.getWorkedMinutes() < HALF_DAY_MINUTES) a.setStatus(AttendanceStatus.HALF_DAY);
        return a;
    }

    @Transactional(readOnly = true)
    public Optional<Attendance> today(Employee e) {
        return attendance.findByEmployeeIdAndWorkDate(e.getId(), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Attendance> forMonth(Employee e, YearMonth ym) {
        return attendance.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(e.getId(), ym.atDay(1), ym.atEndOfMonth());
    }

    /** Monthly counts for the employee's own summary tiles. */
    public Map<String, Long> summarise(List<Attendance> records) {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("present", records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count());
        m.put("late", records.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count());
        m.put("halfDay", records.stream().filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY).count());
        long minutes = records.stream().mapToLong(Attendance::getWorkedMinutes).sum();
        m.put("hours", minutes / 60);
        return m;
    }

    /** Board for one day: every active employee with their status (present, late, half day, on leave, absent, weekend). */
    @Transactional(readOnly = true)
    public List<AttendanceRow> board(LocalDate day) {
        Map<Long, Attendance> byEmployee = attendance.findByDay(day).stream()
                .collect(Collectors.toMap(a -> a.getEmployee().getId(), a -> a, (x, y) -> x));
        Set<Long> onLeave = leaves.findCovering(LeaveStatus.APPROVED, day).stream()
                .map(l -> l.getEmployee().getId()).collect(Collectors.toSet());
        boolean weekend = day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean future = day.isAfter(LocalDate.now());
        List<AttendanceRow> rows = new ArrayList<>();
        for (Employee e : employees.search("", 0L)) {
            if (!e.isActive()) continue;
            Attendance a = byEmployee.get(e.getId());
            String status;
            if (a != null) status = a.getStatus().name();
            else if (onLeave.contains(e.getId())) status = "ON_LEAVE";
            else if (weekend) status = "WEEKEND";
            else if (future) status = "UPCOMING";
            else if (day.equals(LocalDate.now())) status = "NOT_IN_YET";
            else status = "ABSENT";
            rows.add(new AttendanceRow(e, a, status));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public long presentToday() {
        return attendance.findByDay(LocalDate.now()).size();
    }

    @Transactional(readOnly = true)
    public List<Attendance> between(LocalDate from, LocalDate to) { return attendance.findBetween(from, to); }
}
