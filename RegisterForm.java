package com.smarthr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterForm {

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

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Use 8 to 72 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "Include at least one letter and one digit")
    private String password;

    @NotBlank(message = "Confirm your password")
    private String confirmPassword;

    private Long departmentId;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
