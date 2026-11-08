package com.smarthr.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "leave_balances",
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "balance_year", "leave_type"}))
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "balance_year", nullable = false)
    private int balanceYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(nullable = false)
    private int allocated;

    @Column(nullable = false)
    private int used;

    public LeaveBalance() {}

    public LeaveBalance(Employee employee, int balanceYear, LeaveType leaveType, int allocated) {
        this.employee = employee;
        this.balanceYear = balanceYear;
        this.leaveType = leaveType;
        this.allocated = allocated;
    }

    public int getRemaining() { return allocated - used; }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public int getBalanceYear() { return balanceYear; }
    public LeaveType getLeaveType() { return leaveType; }
    public int getAllocated() { return allocated; }
    public void setAllocated(int allocated) { this.allocated = allocated; }
    public int getUsed() { return used; }
    public void setUsed(int used) { this.used = used; }
}
