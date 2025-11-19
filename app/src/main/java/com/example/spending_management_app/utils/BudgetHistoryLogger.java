package com.example.spending_management_app.utils;

import android.content.Context;

import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.BudgetHistoryEntity;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class BudgetHistoryLogger {
    
    /**
     * Log when monthly budget is created
     */
    public static void logMonthlyBudgetCreated(Context context, long amount, Date budgetDate) {
        String description = String.format(Locale.getDefault(),
                "Đặt ngân sách tháng: %,d VND",
                amount);
        
        logBudgetHistory(context, "create", "monthly", null, amount, new Date(), description);
    }
    
    /**
     * Log when monthly budget is updated
     */
    public static void logMonthlyBudgetUpdated(Context context, long oldAmount, long newAmount, Date budgetDate) {
        // Calculate the change (delta)
        long delta = newAmount - oldAmount;
        
        String description;
        if (delta > 0) {
            // Increased
            description = String.format(Locale.getDefault(),
                    "Tăng ngân sách tháng: %,d VND → %,d VND (+%,d VND)",
                    oldAmount, newAmount, delta);
        } else if (delta < 0) {
            // Decreased
            description = String.format(Locale.getDefault(),
                    "Giảm ngân sách tháng: %,d VND → %,d VND (%,d VND)",
                    oldAmount, newAmount, delta);
        } else {
            // No change
            description = String.format(Locale.getDefault(),
                    "Cập nhật ngân sách tháng: %,d VND",
                    newAmount);
        }
        
        // Store delta instead of newAmount
        // Positive delta = increase (green), Negative delta = decrease (red)
        logBudgetHistory(context, "update", "monthly", null, delta, new Date(), description);
    }
    
    /**
     * Log when monthly budget is deleted
     */
    public static void logMonthlyBudgetDeleted(Context context, long amount, Date budgetDate) {
        String description = String.format(Locale.getDefault(),
                "Xóa ngân sách tháng: %,d VND",
                amount);
        
        logBudgetHistory(context, "delete", "monthly", null, amount, new Date(), description);
    }
    
    /**
     * Log when category budget is created
     */
    public static void logCategoryBudgetCreated(Context context, String category, long amount) {
        String description = String.format(Locale.getDefault(),
                "Đặt ngân sách danh mục '%s': %,d VND",
                category, amount);
        
        logBudgetHistory(context, "create", "category", category, amount, new Date(), description);
    }
    
    /**
     * Log when category budget is updated
     */
    public static void logCategoryBudgetUpdated(Context context, String category, long oldAmount, long newAmount) {
        // Calculate the change (delta)
        long delta = newAmount - oldAmount;
        
        String description;
        if (delta > 0) {
            // Increased
            description = String.format(Locale.getDefault(),
                    "Tăng ngân sách '%s': %,d VND → %,d VND (+%,d VND)",
                    category, oldAmount, newAmount, delta);
        } else if (delta < 0) {
            // Decreased
            description = String.format(Locale.getDefault(),
                    "Giảm ngân sách '%s': %,d VND → %,d VND (%,d VND)",
                    category, oldAmount, newAmount, delta);
        } else {
            // No change
            description = String.format(Locale.getDefault(),
                    "Cập nhật ngân sách '%s': %,d VND",
                    category, newAmount);
        }
        
        // Store delta instead of newAmount
        // Positive delta = increase (green), Negative delta = decrease (red)
        logBudgetHistory(context, "update", "category", category, delta, new Date(), description);
    }
    
    /**
     * Log when category budget is deleted
     */
    public static void logCategoryBudgetDeleted(Context context, String category, long amount) {
        String description = String.format(Locale.getDefault(),
                "Xóa ngân sách danh mục '%s': %,d VND",
                category, amount);
        
        logBudgetHistory(context, "delete", "category", category, amount, new Date(), description);
    }
    
    /**
     * Log when all category budgets are deleted
     */
    public static void logAllCategoryBudgetsDeleted(Context context) {
        String description = "Xóa tất cả ngân sách danh mục";
        
        logBudgetHistory(context, "delete", "category", "Tất cả danh mục", 0, new Date(), description);
    }
    
    /**
     * Internal method to save budget history to database
     */
    private static void logBudgetHistory(Context context, String action, String budgetType, 
                                        String category, long amount, Date date, String description) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                BudgetHistoryEntity history = new BudgetHistoryEntity(
                        action, budgetType, category, amount, date, description
                );
                
                AppDatabase.getInstance(context)
                        .budgetHistoryDao()
                        .insert(history);
                
                android.util.Log.d("BudgetHistoryLogger", "Logged: " + description);
            } catch (Exception e) {
                android.util.Log.e("BudgetHistoryLogger", "Error logging budget history", e);
            }
        });
    }
}
