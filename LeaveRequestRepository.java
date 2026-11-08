package com.smarthr.repository;

import com.smarthr.entity.Employee;
import com.smarthr.entity.LeaveRequest;
import com.smarthr.entity.LeaveStatus;
import com.smarthr.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeOrderByAppliedOnDesc(Employee employee);

    @Query("select l from LeaveRequest l join fetch l.employee e where l.status = :status order by l.appliedOn desc")
    List<LeaveRequest> findByStatusFetched(@Param("status") LeaveStatus status);

    @Query("select l from LeaveRequest l join fetch l.employee e order by l.appliedOn desc")
    List<LeaveRequest> findAllFetched();

    long countByStatus(LeaveStatus status);

    List<LeaveRequest> findByEmployeeIdAndLeaveTypeAndStatus(Long employeeId, LeaveType type, LeaveStatus status);

    @Query("select count(l) from LeaveRequest l where l.employee.id = :eid and l.status in :statuses "
         + "and l.startDate <= :end and l.endDate >= :start")
    long countOverlaps(@Param("eid") Long employeeId, @Param("statuses") Collection<LeaveStatus> statuses,
                       @Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Leaves of the given status that cover a single day (used for "on leave today"). */
    @Query("select l from LeaveRequest l join fetch l.employee where l.status = :status "
         + "and l.startDate <= :day and l.endDate >= :day")
    List<LeaveRequest> findCovering(@Param("status") LeaveStatus status, @Param("day") LocalDate day);

    @Query("select l from LeaveRequest l where l.employee.id = :eid and l.leaveType = :type and l.status = :status "
         + "and l.startDate <= :end and l.endDate >= :start")
    List<LeaveRequest> findOverlappingOfType(@Param("eid") Long employeeId, @Param("type") LeaveType type,
                                             @Param("status") LeaveStatus status,
                                             @Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Raw material for analytics and forecasting: every leave of a status that starts on/after a date. */
    @Query("select l from LeaveRequest l join fetch l.employee e left join fetch e.department "
         + "where l.status = :status and l.startDate >= :from")
    List<LeaveRequest> findSince(@Param("status") LeaveStatus status, @Param("from") LocalDate from);

    @Query("select l from LeaveRequest l join fetch l.employee e left join fetch e.department "
         + "where l.startDate >= :from and l.startDate <= :to order by l.startDate")
    List<LeaveRequest> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
