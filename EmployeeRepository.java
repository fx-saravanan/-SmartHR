package com.smarthr.repository;

import com.smarthr.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUserUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Employee> findByActiveTrue();

    long countByActiveTrue();

    long countByDepartmentId(Long departmentId);

    @Query("select coalesce(max(e.id), 0L) from Employee e")
    Long findMaxId();

    /** kw = "" and deptId = 0 mean "no filter". */
    @Query("""
           select e from Employee e left join fetch e.department d
           where (:deptId = 0L or d.id = :deptId)
             and (:kw = '' or lower(e.firstName) like lower(concat('%', :kw, '%'))
                           or lower(e.lastName)  like lower(concat('%', :kw, '%'))
                           or lower(e.email)     like lower(concat('%', :kw, '%'))
                           or lower(e.employeeCode) like lower(concat('%', :kw, '%')))
           order by e.firstName, e.lastName
           """)
    List<Employee> search(@Param("kw") String kw, @Param("deptId") Long deptId);
}
