package com.smarthr.dto;

import com.smarthr.entity.Employee;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileForm {

    @NotBlank(message = "First name is required")
    @Size(max = 60)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 60)
    private String lastName;

    @Pattern(regexp = "^$|^[0-9+\\- ]{7,20}$", message = "Enter a valid phone number")
    private String phone;

    @Size(max = 255)
    private String address;

    public static ProfileForm from(Employee e) {
        ProfileForm f = new ProfileForm();
        f.firstName = e.getFirstName();
        f.lastName = e.getLastName();
        f.phone = e.getPhone();
        f.address = e.getAddress();
        return f;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
