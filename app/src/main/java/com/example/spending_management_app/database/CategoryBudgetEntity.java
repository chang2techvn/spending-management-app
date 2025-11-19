package com.example.spending_management_app.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "category_budgets")
public class CategoryBudgetEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    public String category;
    public long budgetAmount;
    public Date date;
    
    public CategoryBudgetEntity(String category, long budgetAmount, Date date) {
        this.category = category;
        this.budgetAmount = budgetAmount;
        this.date = date;
    }
    
    public long getId() {
        return id;
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
