package com.example.spending_management_app.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.spending_management_app.data.local.converter.DateConverter;

import java.util.Date;

@Entity(tableName = "budget_history")
@TypeConverters({DateConverter.class})
public class BudgetHistoryEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String action; // "create", "update", "delete"
    public String budgetType; // "monthly" or "category"
    public String category; // For category budget, null for monthly budget
    public long amount; // Budget amount
    public Date date; // When the action was performed
    public String description; // Auto-generated description
    
    public BudgetHistoryEntity(String action, String budgetType, String category, long amount, Date date, String description) {
        this.action = action;
        this.budgetType = budgetType;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.description = description;
    }
    
    // Getters
    public int getId() { return id; }
    public String getAction() { return action; }
    public String getBudgetType() { return budgetType; }
    public String getCategory() { return category; }
    public long getAmount() { return amount; }
    public Date getDate() { return date; }
    public String getDescription() { return description; }
}
