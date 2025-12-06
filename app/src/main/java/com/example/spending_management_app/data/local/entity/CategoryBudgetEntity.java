package com.example.spending_management_app.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "category_budgets")
public class CategoryBudgetEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    public int userId; // ID của user sở hữu category budget này
    public String category;
    public long budgetAmount;
    public Date date;
    
    public CategoryBudgetEntity(String category, long budgetAmount, Date date) {
        this.category = category;
        this.budgetAmount = budgetAmount;
        this.date = date;
        this.userId = 1; // Default user ID
    }
    
    public long getId() {
        return id;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getCategory() {
        return category;
    }
    
    public long getBudgetAmount() {
        return budgetAmount;
    }
    
    public Date getDate() {
        return date;
    }
}
