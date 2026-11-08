package com.smarthr.service;

import com.smarthr.dto.LeaveForm;
import com.smarthr.entity.*;
import com.smarthr.exception.BusinessException;
import com.smarthr.exception.ResourceNotFoundException;
import com.smarthr.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class LeaveService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final LeaveRequestRepository leaves;
    private final LeaveBalanceService balances;
    private final NotificationService notifications;

    public LeaveService(LeaveRequestRepository leaves, LeaveBalanceService balances, NotificationService notifications) {
        this.leaves = leaves;
        this.balances = balances;
        this.notifications = notifications;
    }

    /** Working days (Monday to Friday) in the inclusive range. Public holidays are not modelled. */
    public static int countWorkingDays(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) return 0;
        return (int) start.datesUntil(end.plusDays(1))
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();
    }

    public LeaveRequest apply(Employee employee, LeaveForm form) {
        LocalDate start = form.getStartDate();
        LocalDate end = form.getEndDate();
        LeaveType type = form.getLeaveType();
        LocalDate today = LocalDate.now();

        if (end.isBefore(start)) throw new BusinessException("The end date cannot be before the start date.");
        if (start.getYear() != end.getYear()) {
            throw new BusinessException("A request must stay within one calendar year. Split it into two requests.");
        }
        if (type == LeaveType.SICK) {
            if (start.isBefore(today.minusDays(30))) throw new BusinessException("Sick leave can be back-dated by at most 30 days.");
        } else if (start.isBefore(today)) {
            throw new BusinessException("Only sick leave can be applied for past dates.");
        }
        int days = countWorkingDays(start, end);
        if (days == 0) throw new BusinessException("The selected dates fall on a weekend, so there is nothing to apply for.");
        if (leaves.countOverlaps(employee.getId(), List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED), start, end) > 0) {
            throw new BusinessException("You already have a pending or approved leave overlapping these dates.");
        }
        if (type.isPaid()) {
            LeaveBalance b = balances.getOrCreate(employee, start.getYear(), type);
            int available = b.getRemaining() - balances.pendingDays(employee.getId(), start.getYear(), type);
            if (days > available) {
                throw new BusinessException("Not enough " + type.getLabel().toLowerCase() + ": you asked for " + days
                        + " day(s) but " + Math.max(available, 0) + " are available (after pending requests).");
            }
        }
        LeaveRequest r = new LeaveRequest();
        r.setEmployee(employee);
        r.setLeaveType(type);
        r.setStartDate(start);
        r.setEndDate(end);
        r.setDays(days);
        r.setReason(form.getReason().trim());
        r.setStatus(LeaveStatus.PENDING);
        r.setAppliedOn(LocalDateTime.now());
        leaves.save(r);
        notifications.notifyAdmins(NotificationType.LEAVE_APPLIED, employee.getFullName() + " applied for " + days + " day(s) of "
                + type.getLabel().toLowerCase() + " (" + FMT.format(start) + " to " + FMT.format(end) + ").");
        return r;
    }

    public LeaveRequest approve(Long id, String adminUsername, String remark) {
        LeaveRequest r = find(id);
        requirePending(r);
        if (r.getLeaveType().isPaid()) {
            LeaveBalance b = balances.getOrCreate(r.getEmployee(), r.getStartDate().getYear(), r.getLeaveType());
            if (b.getRemaining() < r.getDays()) {
                throw new BusinessException(r.getEmployee().getFullName() + " has only " + b.getRemaining() + " day(s) of "
                        + r.getLeaveType().getLabel().toLowerCase() + " left, so this request cannot be approved.");
            }
            b.setUsed(b.getUsed() + r.getDays());
        }
        decide(r, LeaveStatus.APPROVED, adminUsername, remark);
        notifications.notify(r.getEmployee().getUser(), NotificationType.LEAVE_APPROVED,
                "Your " + r.getLeaveType().getLabel().toLowerCase() + " from " + FMT.format(r.getStartDate()) + " to "
                        + FMT.format(r.getEndDate()) + " was approved." + remarkSuffix(remark));
        return r;
    }

    public LeaveRequest reject(Long id, String adminUsername, String remark) {
        LeaveRequest r = find(id);
        requirePending(r);
        decide(r, LeaveStatus.REJECTED, adminUsername, remark);
        notifications.notify(r.getEmployee().getUser(), NotificationType.LEAVE_REJECTED,
                "Your " + r.getLeaveType().getLabel().toLowerCase() + " from " + FMT.format(r.getStartDate()) + " to "
                        + FMT.format(r.getEndDate()) + " was rejected." + remarkSuffix(remark));
        return r;
    }

    /** Employees may withdraw pending requests, or approved ones that have not started yet (balance is restored). */
    public LeaveRequest cancel(Long id, Employee employee) {
        LeaveRequest r = find(id);
        if (!r.getEmployee().getId().equals(employee.getId())) throw new ResourceNotFoundException("Leave request " + id + " was not found.");
        boolean pending = r.getStatus() == LeaveStatus.PENDING;
        boolean approvedFuture = r.getStatus() == LeaveStatus.APPROVED && r.getStartDate().isAfter(LocalDate.now());
        if (!pending && !approvedFuture) throw new BusinessException("Only pending or upcoming approved leave can be cancelled.");
        if (approvedFuture && r.getLeaveType().isPaid()) {
            LeaveBalance b = balances.getOrCreate(employee, r.getStartDate().getYear(), r.getLeaveType());
            b.setUsed(Math.max(0, b.getUsed() - r.getDays()));
        }
        r.setStatus(LeaveStatus.CANCELLED);
        r.setDecidedOn(LocalDateTime.now());
        notifications.notifyAdmins(NotificationType.LEAVE_CANCELLED, employee.getFullName() + " cancelled the "
                + r.getLeaveType().getLabel().toLowerCase() + " starting " + FMT.format(r.getStartDate()) + ".");
        return r;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> forEmployee(Employee e) { return leaves.findByEmployeeOrderByAppliedOnDesc(e); }

    @Transactional(readOnly = true)
    public List<LeaveRequest> list(LeaveStatus status) {
        return status == null ? leaves.findAllFetched() : leaves.findByStatusFetched(status);
    }

    @Transactional(readOnly = true)
    public long countPending() { return leaves.countByStatus(LeaveStatus.PENDING); }

    private LeaveRequest find(Long id) {
        return leaves.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave request " + id + " was not found."));
    }

    private void requirePending(LeaveRequest r) {
        if (r.getStatus() != LeaveStatus.PENDING) throw new BusinessException("This request has already been " + r.getStatus().name().toLowerCase() + ".");
    }

    private void decide(LeaveRequest r, LeaveStatus status, String admin, String remark) {
        r.setStatus(status);
        r.setDecidedOn(LocalDateTime.now());
        r.setDecidedBy(admin);
        r.setAdminRemark(remark == null || remark.isBlank() ? null : remark.trim());
    }

    private static String remarkSuffix(String remark) {
        return remark == null || remark.isBlank() ? "" : " Remark: " + remark.trim();
    }
}
