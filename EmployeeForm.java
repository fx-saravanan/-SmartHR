package com.smarthr.dto;

import com.smarthr.entity.Employee;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeForm {

    private Long id;

    @NotBlank(message = "First name is required")
    @Size(max = 60)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 60)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 150)
    private String email;

    @Pattern(regexp = "^$|^[0-9+\\- ]{7,20}$", message = "Enter a valid phone number")
    private String phone;

    @Size(max = 255)
    private String address;

    @NotBlank(message = "Designation is required")
    @Size(max = 100)
    private String designation;

    @NotNull(message = "Joining date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate joiningDate;

    private Long departmentId;

    @NotNull(message = "Basic salary is required")
    @DecimalMin(value = "0.00", message = "Cannot be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @NotNull(message = "HRA is required")
    @DecimalMin(value = "0.00", message = "Cannot be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal hra = BigDecimal.ZERO;

    @NotNull(message = "Allowances are required")
    @DecimalMin(value = "0.00", message = "Cannot be negative")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal allowances = BigDecimal.ZERO;

    /** Required when creating, ignored when blank while editing. */
    @Size(max = 72, message = "Use at most 72 characters")
    private String password;

    private boolean active = true;

    public static EmployeeForm from(Employee e) {
        EmployeeForm f = new EmployeeForm();
        f.id = e.getId();
        f.firstName = e.getFirstName();
        f.lastName = e.getLastName();
        f.email = e.getEmail();
        f.phone = e.getPhone();
        f.address = e.getAddress();
        f.designation = e.getDesignation();
        f.joiningDate = e.getJoiningDate();
        f.departmentId = e.getDepartment() == null ? null : e.getDepartment().getId();
        f.basicSalary = e.getBasicSalary();
        f.hra = e.getHra();
        f.allowances = e.getAllowances();
        f.active = e.isActive();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    public BigDecimal getHra() { return hra; }
    public void setHra(BigDecimal hra) { this.hra = hra; }
    public BigDecimal getAllowances() { return allowances; }
    public void setAllowances(BigDecimal allowances) { this.allowances = allowances; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
