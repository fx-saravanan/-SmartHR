package com.smarthr.repository;

import com.smarthr.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate date);

    List<Attendance> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(Long employeeId, LocalDate from, LocalDate to);

    @Query("select a from Attendance a join fetch a.employee where a.workDate = :day")
    List<Attendance> findByDay(@Param("day") LocalDate day);

    @Query("select a from Attendance a join fetch a.employee e where a.workDate between :from and :to order by a.workDate, e.firstName")
    List<Attendance> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
