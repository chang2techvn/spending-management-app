package com.example.spending_management_app.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class BudgetEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String category;
    public long monthlyLimit;
    public long currentSpent;

    public BudgetEntity(String category, long monthlyLimit, long currentSpent) {
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.currentSpent = currentSpent;
    }
}