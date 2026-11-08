package com.smarthr.service;

import com.smarthr.dto.LeaveBalanceView;
import com.smarthr.entity.*;
import com.smarthr.repository.EmployeeRepository;
import com.smarthr.repository.LeaveBalanceRepository;
import com.smarthr.repository.LeaveRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Automated leave balance handling: yearly allocation, pro-rating for new joiners, carry-forward. */
@Service
@Transactional
public class LeaveBalanceService {

    private static final Logger log = LoggerFactory.getLogger(LeaveBalanceService.class);
    /** Maximum earned-leave days carried into the next year. */
    public static final int EARNED_CARRY_FORWARD_CAP = 5;

    private final LeaveBalanceRepository balances;
    private final LeaveRequestRepository leaves;
    private final EmployeeRepository employees;

    public LeaveBalanceService(LeaveBalanceRepository balances, LeaveRequestRepository leaves, EmployeeRepository employees) {
        this.balances = balances;
        this.leaves = leaves;
        this.employees = employees;
    }

    /** Annual quota, pro-rated by remaining months when the employee joined mid-year. */
    public int allocationFor(Employee e, int year, LeaveType type) {
        int quota = type.getAnnualQuota();
        LocalDate joined = e.getJoiningDate();
        if (joined != null && joined.getYear() == year && joined.getMonthValue() > 1) {
            int months = 12 - joined.getMonthValue() + 1;
            quota = (int) Math.round(quota * months / 12.0);
        }
        return quota;
    }

    public LeaveBalance getOrCreate(Employee e, int year, LeaveType type) {
        return balances.findByEmployeeIdAndBalanceYearAndLeaveType(e.getId(), year, type)
                .orElseGet(() -> balances.save(new LeaveBalance(e, year, type, allocationFor(e, year, type))));
    }

    public void ensureBalances(Employee e, int year) {
        for (LeaveType t : LeaveType.values()) if (t.isPaid()) getOrCreate(e, year, t);
    }

    @Transactional(readOnly = true)
    public int pendingDays(Long employeeId, int year, LeaveType type) {
        return leaves.findByEmployeeIdAndLeaveTypeAndStatus(employeeId, type, LeaveStatus.PENDING).stream()
                .filter(l -> l.getStartDate().getYear() == year).mapToInt(LeaveRequest::getDays).sum();
    }

    public List<LeaveBalanceView> viewsFor(Employee e, int year) {
        List<LeaveBalanceView> views = new ArrayList<>();
        for (LeaveType t : LeaveType.values()) {
            if (!t.isPaid()) continue;
            LeaveBalance b = getOrCreate(e, year, t);
            views.add(new LeaveBalanceView(t, b.getAllocated(), b.getUsed(), pendingDays(e.getId(), year, t)));
        }
        return views;
    }

    /** Allocates the new year's balances for everyone, carrying forward unused earned leave (capped). */
    public int allocateYear(int year) {
        int count = 0;
        for (Employee e : employees.findByActiveTrue()) {
            for (LeaveType t : LeaveType.values()) {
                if (!t.isPaid()) continue;
                if (balances.findByEmployeeIdAndBalanceYearAndLeaveType(e.getId(), year, t).isPresent()) continue;
                int allocated = allocationFor(e, year, t);
                if (t == LeaveType.EARNED) {
                    int carry = balances.findByEmployeeIdAndBalanceYearAndLeaveType(e.getId(), year - 1, t)
                            .map(LeaveBalance::getRemaining).orElse(0);
                    allocated += Math.max(0, Math.min(EARNED_CARRY_FORWARD_CAP, carry));
                }
                balances.save(new LeaveBalance(e, year, t, allocated));
                count++;
            }
        }
        return count;
    }

    /** Runs at 00:05 on 1 January. */
    @Scheduled(cron = "0 5 0 1 1 *")
    public void yearlyRollover() {
        int year = LocalDate.now().getYear();
        log.info("Allocating leave balances for {}: {} new records", year, allocateYear(year));
    }
}
