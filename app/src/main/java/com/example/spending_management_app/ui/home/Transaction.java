package com.example.spending_management_app.ui.home;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Transaction {
    private String description;
    private String category;
    private long amount;
    private String iconResName;
    private Date date;
    private String type; // "expense" or "income"

    public Transaction(String description, String category, long amount, String iconResName, Date date, String type) {
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.iconResName = iconResName;
        this.date = date;
        this.type = type;
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

    public String getIconResName() {
        return iconResName;
    }

    public Date getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    public String getFormattedAmount() {
        if (amount >= 0) {
            return "+" + formatCurrency(amount);
        } else {
            return "-" + formatCurrency(amount);
        }
    }

    private String formatCurrency(long amount) {
        // Simple currency formatting - you can enhance this
        String amountStr = String.valueOf(Math.abs(amount));
        StringBuilder formatted = new StringBuilder();
        int count = 0;
        for (int i = amountStr.length() - 1; i >= 0; i--) {
            formatted.insert(0, amountStr.charAt(i));
            count++;
            if (count % 3 == 0 && i > 0) {
                formatted.insert(0, ",");
            }
        }
        return formatted.toString() + " VND";
    }
}