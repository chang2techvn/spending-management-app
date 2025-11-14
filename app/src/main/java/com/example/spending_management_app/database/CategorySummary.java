package com.example.spending_management_app.database;

public class CategorySummary {
    public String category;
    public long total;

    public CategorySummary(String category, long total) {
        this.category = category;
        this.total = total;
    }
}