package com.example.spending_management_app.data.local.entity;

public class CategorySummary {
    public String category;
    public long total;

    public CategorySummary(String category, long total) {
        this.category = category;
        this.total = total;
    }
}