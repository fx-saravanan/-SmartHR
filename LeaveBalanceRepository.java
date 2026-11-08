package com.smarthr.repository;

import com.smarthr.entity.LeaveBalance;
import com.smarthr.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByEmployeeIdAndBalanceYearAndLeaveType(Long employeeId, int year, LeaveType type);
    List<LeaveBalance> findByEmployeeIdAndBalanceYear(Long employeeId, int year);
}
