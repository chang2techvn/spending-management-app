package com.example.spending_management_app.domain.model;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.content.Context;
import com.example.spending_management_app.utils.CurrencyFormatter;

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

    public String getFormattedAmount(Context context) {
        if (amount >= 0) {
            return "+" + CurrencyFormatter.formatCurrency(context, Math.abs(amount));
        } else {
            return "-" + CurrencyFormatter.formatCurrency(context, Math.abs(amount));
        }
    }


}