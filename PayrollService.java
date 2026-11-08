package com.smarthr.service;

import com.smarthr.entity.*;
import com.smarthr.exception.BusinessException;
import com.smarthr.exception.ResourceNotFoundException;
import com.smarthr.repository.EmployeeRepository;
import com.smarthr.repository.LeaveRequestRepository;
import com.smarthr.repository.PayslipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Monthly payroll: gross = basic + HRA + allowances; deductions = provident fund (12% of basic),
 * an illustrative income-tax slab, and loss-of-pay for approved unpaid leave days.
 * Replace {@link #incomeTax(BigDecimal)} with your jurisdiction's rules before real use.
 */
@Service
@Transactional
public class PayrollService {

    private static final BigDecimal PF_RATE = new BigDecimal("0.12");

    private final PayslipRepository payslips;
    private final EmployeeRepository employees;
    private final LeaveRequestRepository leaves;
    private final NotificationService notifications;

    public PayrollService(PayslipRepository payslips, EmployeeRepository employees, LeaveRequestRepository leaves,
                          NotificationService notifications) {
        this.payslips = payslips;
        this.employees = employees;
        this.leaves = leaves;
        this.notifications = notifications;
    }

    /** Result of a payroll run. */
    public record RunSummary(int created, int updated, int skipped) {}

    public RunSummary generate(int year, int month, boolean overwrite) {
        YearMonth ym = YearMonth.of(year, month);
        int created = 0, updated = 0, skipped = 0;
        for (Employee e : employees.findByActiveTrue()) {
            if (e.getBasicSalary().signum() <= 0) { skipped++; continue; }
            if (e.getJoiningDate() != null && e.getJoiningDate().isAfter(ym.atEndOfMonth())) { skipped++; continue; }
            Optional<Payslip> existing = payslips.findByEmployeeIdAndPayYearAndPayMonth(e.getId(), year, month);
            if (existing.isPresent() && (!overwrite || existing.get().getStatus() == PayslipStatus.PAID)) { skipped++; continue; }
            Payslip p = existing.orElseGet(Payslip::new);
            fill(p, e, ym);
            payslips.save(p);
            if (existing.isPresent()) updated++; else created++;
            if (existing.isEmpty()) {
                notifications.notify(e.getUser(), NotificationType.PAYSLIP, "Your salary slip for " + p.getPeriodLabel() + " is ready to download.");
            }
        }
        return new RunSummary(created, updated, skipped);
    }

    private void fill(Payslip p, Employee e, YearMonth ym) {
        LocalDate first = ym.atDay(1), last = ym.atEndOfMonth();
        int workingDays = LeaveService.countWorkingDays(first, last);
        int unpaidDays = 0;
        for (LeaveRequest l : leaves.findOverlappingOfType(e.getId(), LeaveType.UNPAID, LeaveStatus.APPROVED, first, last)) {
            LocalDate s = l.getStartDate().isBefore(first) ? first : l.getStartDate();
            LocalDate en = l.getEndDate().isAfter(last) ? last : l.getEndDate();
            unpaidDays += LeaveService.countWorkingDays(s, en);
        }
        BigDecimal gross = e.getGrossSalary();
        BigDecimal perDay = gross.divide(BigDecimal.valueOf(workingDays), 2, RoundingMode.HALF_UP);
        BigDecimal unpaidDeduction = perDay.multiply(BigDecimal.valueOf(unpaidDays)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pf = e.getBasicSalary().multiply(PF_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = incomeTax(gross);
        BigDecimal net = gross.subtract(pf).subtract(tax).subtract(unpaidDeduction).max(BigDecimal.ZERO);

        p.setEmployee(e);
        p.setPayYear(ym.getYear());
        p.setPayMonth(ym.getMonthValue());
        p.setWorkingDays(workingDays);
        p.setUnpaidLeaveDays(unpaidDays);
        p.setBasic(e.getBasicSalary());
        p.setHra(e.getHra());
        p.setAllowances(e.getAllowances());
        p.setGrossPay(gross);
        p.setPfDeduction(pf);
        p.setTaxDeduction(tax);
        p.setUnpaidDeduction(unpaidDeduction);
        p.setNetPay(net.setScale(2, RoundingMode.HALF_UP));
        p.setGeneratedOn(LocalDateTime.now());
    }

    /** Illustrative monthly slab: nothing up to 25,000; 5% of the part up to 50,000; 10% above that. */
    static BigDecimal incomeTax(BigDecimal monthlyGross) {
        BigDecimal t1 = new BigDecimal("25000"), t2 = new BigDecimal("50000");
        BigDecimal tax = BigDecimal.ZERO;
        if (monthlyGross.compareTo(t1) > 0) tax = tax.add(monthlyGross.min(t2).subtract(t1).multiply(new BigDecimal("0.05")));
        if (monthlyGross.compareTo(t2) > 0) tax = tax.add(monthlyGross.subtract(t2).multiply(new BigDecimal("0.10")));
        return tax.setScale(2, RoundingMode.HALF_UP);
    }

    public Payslip markPaid(Long id) {
        Payslip p = get(id);
        if (p.getStatus() == PayslipStatus.PAID) throw new BusinessException("This salary slip is already marked as paid.");
        p.setStatus(PayslipStatus.PAID);
        notifications.notify(p.getEmployee().getUser(), NotificationType.PAYSLIP, "Your salary for " + p.getPeriodLabel() + " has been paid.");
        return p;
    }

    @Transactional(readOnly = true)
    public Payslip get(Long id) {
        return payslips.findById(id).orElseThrow(() -> new ResourceNotFoundException("Salary slip " + id + " was not found."));
    }

    @Transactional(readOnly = true)
    public List<Payslip> forPeriod(int year, int month) { return payslips.findForPeriod(year, month); }

    @Transactional(readOnly = true)
    public List<Payslip> forEmployee(Employee e) { return payslips.findByEmployeeIdOrderByPayYearDescPayMonthDesc(e.getId()); }
}
