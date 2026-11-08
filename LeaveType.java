package com.smarthr.entity;

public enum LeaveType {
    CASUAL("Casual leave", 12),
    SICK("Sick leave", 10),
    EARNED("Earned leave", 15),
    UNPAID("Unpaid leave", 0);

    private final String label;
    private final int annualQuota;

    LeaveType(String label, int annualQuota) {
        this.label = label;
        this.annualQuota = annualQuota;
    }

    public String getLabel() { return label; }
    public int getAnnualQuota() { return annualQuota; }
    /** Paid leave types draw down a balance; unpaid leave does not. */
    public boolean isPaid() { return this != UNPAID; }
}
