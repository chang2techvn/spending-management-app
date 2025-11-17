package com.example.spending_management_app.database;

/**
 * POJO class for monthly spending data
 */
public class MonthlySpending {
    public String month;  // Format: YYYY-MM
    public long total;    // Total spending amount

    public MonthlySpending() {
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
