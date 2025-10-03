package com.example.spending_management_app.ui.home;

public class Transaction {
    private String title;
    private String category;
    private long amount;
    private String iconResName;

    public Transaction(String title, String category, long amount, String iconResName) {
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.iconResName = iconResName;
    }

    public String getTitle() {
        return title;
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

    public String getFormattedAmount() {
        if (amount >= 0) {
            return "+" + formatCurrency(amount);
        } else {
            return formatCurrency(amount);
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