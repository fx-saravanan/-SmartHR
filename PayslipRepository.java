package com.smarthr.repository;

import com.smarthr.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    Optional<Payslip> findByEmployeeIdAndPayYearAndPayMonth(Long employeeId, int year, int month);

    List<Payslip> findByEmployeeIdOrderByPayYearDescPayMonthDesc(Long employeeId);

    @Query("select p from Payslip p join fetch p.employee e where p.payYear = :year and p.payMonth = :month order by e.firstName")
    List<Payslip> findForPeriod(@Param("year") int year, @Param("month") int month);
}
