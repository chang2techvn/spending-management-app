package com.example.spending_management_app.domain.model;

public class Budget {
    private String category;
    private long monthlyLimit;
    private long currentSpent;

    public Budget(String category, long monthlyLimit, long currentSpent) {
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.currentSpent = currentSpent;
    }

    public String getCategory() {
        return category;
    }

    public long getMonthlyLimit() {
        return monthlyLimit;
    }

    public long getCurrentSpent() {
        return currentSpent;
    }

    public long getRemaining() {
        return monthlyLimit - currentSpent;
    }

    public double getSpentPercentage() {
        if (monthlyLimit == 0) return 0;
        return (double) currentSpent / monthlyLimit * 100;
    }

    public void setMonthlyLimit(long monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public void addSpent(long amount) {
        this.currentSpent += amount;
    }
}