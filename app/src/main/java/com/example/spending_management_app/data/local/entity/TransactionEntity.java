package com.example.spending_management_app.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "transactions")
public class TransactionEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String description;
    public String category;
    public long amount;
    public Date date;
    public String type; // "expense" or "income"

    public TransactionEntity(String description, String category, long amount, Date date, String type) {
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.type = type;
    }
}