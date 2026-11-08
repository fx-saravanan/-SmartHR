package com.smarthr.dto;

import com.smarthr.entity.LeaveType;

/** Read-only view of one leave type's balance, including days already requested but not yet decided. */
public class LeaveBalanceView {
    private final LeaveType type;
    private final int allocated;
    private final int used;
    private final int pending;

    public LeaveBalanceView(LeaveType type, int allocated, int used, int pending) {
        this.type = type;
        this.allocated = allocated;
        this.used = used;
        this.pending = pending;
    }

    public LeaveType getType() { return type; }
    public int getAllocated() { return allocated; }
    public int getUsed() { return used; }
    public int getPending() { return pending; }
    public int getRemaining() { return allocated - used; }
    public int getAvailable() { return allocated - used - pending; }
    public int getUsedPercent() { return allocated == 0 ? 0 : Math.min(100, (int) Math.round(used * 100.0 / allocated)); }
}
