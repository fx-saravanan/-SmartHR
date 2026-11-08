package com.smarthr.dto;

import com.smarthr.entity.Attendance;
import com.smarthr.entity.Employee;

/** One line of the admin's daily attendance board. */
public class AttendanceRow {
    private final Employee employee;
    private final Attendance attendance;
    private final String status;

    public AttendanceRow(Employee employee, Attendance attendance, String status) {
        this.employee = employee;
        this.attendance = attendance;
        this.status = status;
    }

    public Employee getEmployee() { return employee; }
    public Attendance getAttendance() { return attendance; }
    public String getStatus() { return status; }
}
