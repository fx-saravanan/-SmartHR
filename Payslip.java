package com.smarthr.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@Entity
@Table(name = "payslips",
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "pay_year", "pay_month"}))
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "pay_year", nullable = false)
    private int payYear;

    @Column(name = "pay_month", nullable = false)
    private int payMonth;

    @Column(name = "working_days", nullable = false)
    private int workingDays;

    @Column(name = "unpaid_leave_days", nullable = false)
    private int unpaidLeaveDays;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal basic = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal hra = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal allowances = BigDecimal.ZERO;
    @Column(name = "gross_pay", nullable = false, precision = 12, scale = 2) private BigDecimal grossPay = BigDecimal.ZERO;
    @Column(name = "pf_deduction", nullable = false, precision = 12, scale = 2) private BigDecimal pfDeduction = BigDecimal.ZERO;
    @Column(name = "tax_deduction", nullable = false, precision = 12, scale = 2) private BigDecimal taxDeduction = BigDecimal.ZERO;
    @Column(name = "unpaid_deduction", nullable = false, precision = 12, scale = 2) private BigDecimal unpaidDeduction = BigDecimal.ZERO;
    @Column(name = "net_pay", nullable = false, precision = 12, scale = 2) private BigDecimal netPay = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayslipStatus status = PayslipStatus.GENERATED;

    @Column(name = "generated_on", nullable = false)
    private LocalDateTime generatedOn = LocalDateTime.now();

    public String getPeriodLabel() {
        return Month.of(payMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + payYear;
    }

    public BigDecimal getTotalDeductions() { return pfDeduction.add(taxDeduction).add(unpaidDeduction); }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public int getPayYear() { return payYear; }
    public void setPayYear(int payYear) { this.payYear = payYear; }
    public int getPayMonth() { return payMonth; }
    public void setPayMonth(int payMonth) { this.payMonth = payMonth; }
    public int getWorkingDays() { return workingDays; }
    public void setWorkingDays(int workingDays) { this.workingDays = workingDays; }
    public int getUnpaidLeaveDays() { return unpaidLeaveDays; }
    public void setUnpaidLeaveDays(int unpaidLeaveDays) { this.unpaidLeaveDays = unpaidLeaveDays; }
    public BigDecimal getBasic() { return basic; }
    public void setBasic(BigDecimal basic) { this.basic = basic; }
    public BigDecimal getHra() { return hra; }
    public void setHra(BigDecimal hra) { this.hra = hra; }
    public BigDecimal getAllowances() { return allowances; }
    public void setAllowances(BigDecimal allowances) { this.allowances = allowances; }
    public BigDecimal getGrossPay() { return grossPay; }
    public void setGrossPay(BigDecimal grossPay) { this.grossPay = grossPay; }
    public BigDecimal getPfDeduction() { return pfDeduction; }
    public void setPfDeduction(BigDecimal pfDeduction) { this.pfDeduction = pfDeduction; }
    public BigDecimal getTaxDeduction() { return taxDeduction; }
    public void setTaxDeduction(BigDecimal taxDeduction) { this.taxDeduction = taxDeduction; }
    public BigDecimal getUnpaidDeduction() { return unpaidDeduction; }
    public void setUnpaidDeduction(BigDecimal unpaidDeduction) { this.unpaidDeduction = unpaidDeduction; }
    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }
    public PayslipStatus getStatus() { return status; }
    public void setStatus(PayslipStatus status) { this.status = status; }
    public LocalDateTime getGeneratedOn() { return generatedOn; }
    public void setGeneratedOn(LocalDateTime generatedOn) { this.generatedOn = generatedOn; }
}
