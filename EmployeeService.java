package com.smarthr.service;

import com.smarthr.dto.EmployeeForm;
import com.smarthr.dto.PasswordForm;
import com.smarthr.dto.ProfileForm;
import com.smarthr.dto.RegisterForm;
import com.smarthr.entity.*;
import com.smarthr.exception.BusinessException;
import com.smarthr.exception.ResourceNotFoundException;
import com.smarthr.repository.AppUserRepository;
import com.smarthr.repository.DepartmentRepository;
import com.smarthr.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employees;
    private final AppUserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;
    private final LeaveBalanceService balances;

    public EmployeeService(EmployeeRepository employees, AppUserRepository users, DepartmentRepository departments,
                           PasswordEncoder encoder, LeaveBalanceService balances) {
        this.employees = employees;
        this.users = users;
        this.departments = departments;
        this.encoder = encoder;
        this.balances = balances;
    }

    @Transactional(readOnly = true)
    public List<Employee> search(String keyword, Long departmentId) {
        String kw = keyword == null ? "" : keyword.trim();
        return employees.search(kw, departmentId == null ? 0L : departmentId);
    }

    @Transactional(readOnly = true)
    public Employee get(Long id) {
        return employees.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee " + id + " was not found."));
    }

    @Transactional(readOnly = true)
    public Employee getByUsername(String username) {
        return employees.findByUserUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("No employee profile is linked to this account."));
    }

    /** Self-registration from the public sign-up page. Salary is left at zero for an admin to set. */
    public Employee register(RegisterForm f) {
        if (!f.getPassword().equals(f.getConfirmPassword())) throw new BusinessException("Passwords do not match.");
        EmployeeForm ef = new EmployeeForm();
        ef.setFirstName(f.getFirstName());
        ef.setLastName(f.getLastName());
        ef.setEmail(f.getEmail());
        ef.setPhone(f.getPhone());
        ef.setDesignation("Employee");
        ef.setJoiningDate(LocalDate.now());
        ef.setDepartmentId(f.getDepartmentId());
        ef.setPassword(f.getPassword());
        return create(ef);
    }

    /** Admin-created employee (also used by registration). */
    public Employee create(EmployeeForm f) {
        String email = f.getEmail().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(email) || employees.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("An account with the email " + email + " already exists.");
        }
        if (f.getPassword() == null || f.getPassword().length() < 8) {
            throw new BusinessException("A password of at least 8 characters is required.");
        }
        Employee e = new Employee();
        apply(e, f);
        e.setEmail(email);
        e.setEmployeeCode(String.format("EMP%04d", employees.findMaxId() + 1));
        e.setUser(new AppUser(email, encoder.encode(f.getPassword()), Role.EMPLOYEE));
        e.getUser().setEnabled(f.isActive());
        Employee saved = employees.save(e);
        balances.ensureBalances(saved, LocalDate.now().getYear());
        return saved;
    }

    public Employee update(EmployeeForm f) {
        Employee e = get(f.getId());
        String email = f.getEmail().trim().toLowerCase();
        if (employees.existsByEmailIgnoreCaseAndIdNot(email, e.getId())) {
            throw new BusinessException("Another employee already uses the email " + email + ".");
        }
        apply(e, f);
        if (!email.equalsIgnoreCase(e.getEmail())) {
            if (users.findByUsernameIgnoreCase(email).filter(u -> !u.getId().equals(e.getUser().getId())).isPresent()) {
                throw new BusinessException("The email " + email + " is already used as a login.");
            }
            e.setEmail(email);
            e.getUser().setUsername(email);
        }
        if (f.getPassword() != null && !f.getPassword().isBlank()) {
            if (f.getPassword().length() < 8) throw new BusinessException("The new password must have at least 8 characters.");
            e.getUser().setPassword(encoder.encode(f.getPassword()));
        }
        e.getUser().setEnabled(f.isActive());
        return employees.save(e);
    }

    private void apply(Employee e, EmployeeForm f) {
        e.setFirstName(f.getFirstName().trim());
        e.setLastName(f.getLastName().trim());
        e.setPhone(blankToNull(f.getPhone()));
        e.setAddress(blankToNull(f.getAddress()));
        e.setDesignation(f.getDesignation().trim());
        e.setJoiningDate(f.getJoiningDate());
        e.setBasicSalary(f.getBasicSalary());
        e.setHra(f.getHra());
        e.setAllowances(f.getAllowances());
        e.setActive(f.isActive());
        e.setDepartment(f.getDepartmentId() == null ? null : departments.findById(f.getDepartmentId())
                .orElseThrow(() -> new BusinessException("The selected department no longer exists.")));
    }

    public void updateProfile(Employee e, ProfileForm f) {
        e.setFirstName(f.getFirstName().trim());
        e.setLastName(f.getLastName().trim());
        e.setPhone(blankToNull(f.getPhone()));
        e.setAddress(blankToNull(f.getAddress()));
        employees.save(e);
    }

    public void changePassword(String username, PasswordForm f) {
        AppUser u = users.findByUsernameIgnoreCase(username).orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        if (!encoder.matches(f.getCurrentPassword(), u.getPassword())) throw new BusinessException("Your current password is incorrect.");
        if (!f.getNewPassword().equals(f.getConfirmPassword())) throw new BusinessException("The new passwords do not match.");
        if (encoder.matches(f.getNewPassword(), u.getPassword())) throw new BusinessException("Choose a password you have not used just now.");
        u.setPassword(encoder.encode(f.getNewPassword()));
    }

    public Employee setActive(Long id, boolean active) {
        Employee e = get(id);
        e.setActive(active);
        e.getUser().setEnabled(active);
        return e;
    }

    @Transactional(readOnly = true)
    public long countActive() { return employees.countByActiveTrue(); }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
