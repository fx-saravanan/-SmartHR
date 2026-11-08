package com.smarthr.dto;

/** Employee whose recent leave usage is well above the workforce average. */
public class EmployeeRisk {
    private final String name;
    private final String department;
    private final int leaveDays;
    private final int sickDays;
    private final double ratioToAverage;

    public EmployeeRisk(String name, String department, int leaveDays, int sickDays, double ratioToAverage) {
        this.name = name;
        this.department = department;
        this.leaveDays = leaveDays;
        this.sickDays = sickDays;
        this.ratioToAverage = ratioToAverage;
    }

    public String getName() { return name; }
    public String getDepartment() { return department; }
    public int getLeaveDays() { return leaveDays; }
    public int getSickDays() { return sickDays; }
    public double getRatioToAverage() { return ratioToAverage; }
}
