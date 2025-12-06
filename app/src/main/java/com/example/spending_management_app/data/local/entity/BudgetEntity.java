package com.example.spending_management_app.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "budgets")
public class BudgetEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId; // ID của user sở hữu budget này
    public String category;
    public long monthlyLimit;
    public long currentSpent;
    public Date date;

    public BudgetEntity(String category, long monthlyLimit, long currentSpent, Date date) {
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.currentSpent = currentSpent;
        this.date = date;
        this.userId = 1; // Default user ID
    }

    // Getters
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getCategory() { return category; }
    public long getMonthlyLimit() { return monthlyLimit; }
    public long getCurrentSpent() { return currentSpent; }
    public Date getDate() { return date; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setCategory(String category) { this.category = category; }
    public void setMonthlyLimit(long monthlyLimit) { this.monthlyLimit = monthlyLimit; }
    public void setCurrentSpent(long currentSpent) { this.currentSpent = currentSpent; }
    public void setDate(Date date) { this.date = date; }
}