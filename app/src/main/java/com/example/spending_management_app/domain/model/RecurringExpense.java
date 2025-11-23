package com.example.spending_management_app.domain.model;

import java.util.Calendar;
import java.util.Date;

public class RecurringExpense {
    private String description;
    private String category;
    private long amount;
    private Date startDate;
    private Date endDate;
    private String frequency; // "monthly", "weekly", "daily"

    public RecurringExpense(String description, String category, long amount, Date startDate, Date endDate, String frequency) {
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.frequency = frequency;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public long getAmount() {
        return amount;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public String getFrequency() {
        return frequency;
    }

    // Method to check if expense should be added for current date
    public boolean shouldAddForDate(Date date) {
        if (date.before(startDate) || (endDate != null && date.after(endDate))) {
            return false;
        }

        // Simple check for monthly (same day of month)
        if ("monthly".equals(frequency)) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startDate);
            int startDayOfMonth = startCal.get(Calendar.DAY_OF_MONTH);

            return dayOfMonth == startDayOfMonth;
        }

        // For simplicity, assume monthly for now
        return true;
    }
}