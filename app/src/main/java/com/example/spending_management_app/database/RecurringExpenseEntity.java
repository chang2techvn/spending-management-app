package com.example.spending_management_app.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "recurring_expenses")
public class RecurringExpenseEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String description;
    public String category;
    public long amount;
    public Date startDate;
    public Date endDate;
    public String frequency;

    public RecurringExpenseEntity(String description, String category, long amount, Date startDate, Date endDate, String frequency) {
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.frequency = frequency;
    }
}