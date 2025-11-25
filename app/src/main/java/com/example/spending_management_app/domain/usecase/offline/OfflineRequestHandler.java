package com.example.spending_management_app.domain.usecase.offline;

import android.content.Context;
import android.util.Log;

import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.local.entity.BudgetEntity;
import com.example.spending_management_app.data.local.entity.TransactionEntity;
import com.example.spending_management_app.data.local.entity.CategoryBudgetEntity;
import com.example.spending_management_app.utils.BudgetAmountParser;
import com.example.spending_management_app.domain.usecase.budget.BudgetHistoryLogger;
import com.example.spending_management_app.utils.CategoryHelper;
import com.example.spending_management_app.utils.CurrencyFormatter;
import com.example.spending_management_app.utils.DateParser;
import com.example.spending_management_app.utils.ExpenseDescriptionParser;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler for offline AI chat requests
 * Processes user requests without requiring network connectivity
 * Supports expense tracking, budget management, and category budget operations
 */
public class OfflineRequestHandler {
    
    private static final String TAG = "OfflineRequestHandler";
    
    private final Context context;
    private final OfflineRequestCallback callback;
    
    /**
     * Callback interface for handling offline request results
     */
    public interface OfflineRequestCallback {
        void onSuccess(String message);
        void onError(String errorMessage);
        void onToast(String toastMessage, boolean isError);
        void refreshHomeFragment();
        void refreshExpenseWelcomeMessage();
        void refreshCategoryBudgetWelcomeMessage();
    }
    
    public OfflineRequestHandler(Context context, OfflineRequestCallback callback) {
        this.context = context;
        this.callback = callback;
    }
    
    /**
     * Main entry point for handling offline requests
     * @param text User input text
     * @param isBudgetMode Whether in budget management mode
     * @param isCategoryBudgetMode Whether in category budget management mode
     * @param isExpenseBulkMode Whether in expense bulk management mode
     * @return true if request was handled, false otherwise
     */
    public boolean handleOfflineRequest(String text, boolean isBudgetMode, 
                                       boolean isCategoryBudgetMode, boolean isExpenseBulkMode) {
        String lowerText = text.toLowerCase();
        
        // Check if text contains a category name (for category budget detection)
        boolean hasCategoryName = false;
        for (String cat : CategoryHelper.getAllCategories()) {
            if (lowerText.contains(cat.toLowerCase())) {
                hasCategoryName = true;
                break;
            }
        }
        
        // 3. Handle category budget operations (check first if category name is present)
        if (isCategoryBudgetMode || hasCategoryName || lowerText.contains("ngân sách danh mục") || lowerText.contains("category budget")) {
            boolean isBudgetOperation = lowerText.contains("ngân sách") || lowerText.contains("category budget") ||
                                       lowerText.contains("đặt") || lowerText.contains("set") ||
                                       lowerText.contains("thêm") || lowerText.contains("add") ||
                                       lowerText.contains("sửa") || lowerText.contains("edit") ||
                                       lowerText.contains("xóa") || lowerText.contains("xoá") ||
                                       lowerText.contains("delete") || lowerText.contains("remove");
            
            if (isBudgetOperation && hasCategoryName) {
                // Delete category budget
                if (lowerText.contains("xóa") || lowerText.contains("xoá") ||
                    lowerText.contains("delete") || lowerText.contains("remove")) {
                    return handleOfflineDeleteCategoryBudget(text);
                }
                // Add/Update category budget
                if (lowerText.contains("thêm") || lowerText.contains("đặt") || 
                    lowerText.contains("sửa") || lowerText.contains("ngân sách") ||
                    lowerText.contains("add") || lowerText.contains("set") ||
                    lowerText.contains("edit") || lowerText.contains("category budget")) {
                    return handleOfflineUpdateCategoryBudget(text);
                }
            }
        }
        
        // 2. Handle monthly budget operations
        if (isBudgetMode || lowerText.contains("ngân sách tháng") || lowerText.contains("monthly budget")) {
            // Delete budget
            if (lowerText.contains("xóa") || lowerText.contains("xoá") ||
                lowerText.contains("delete") || lowerText.contains("remove")) {
                return handleOfflineDeleteBudget(text);
            }
            // Add/Update budget
            if (lowerText.contains("thêm") || lowerText.contains("đặt") || 
                lowerText.contains("sửa") || lowerText.contains("nâng") || 
                lowerText.contains("tăng") || lowerText.contains("giảm") ||
                lowerText.contains("hạ") || lowerText.contains("cắt") ||
                lowerText.contains("trừ") || lowerText.contains("bớt") ||
                lowerText.contains("add") || lowerText.contains("set") ||
                lowerText.contains("edit") || lowerText.contains("increase") ||
                lowerText.contains("decrease") || lowerText.contains("monthly budget")) {
                return handleOfflineUpdateBudget(text);
            }
        }
        
        // 1. Handle expense operations
        if (isExpenseBulkMode || (!isBudgetMode && !isCategoryBudgetMode)) {
            // Delete expense
            if (lowerText.contains("xóa") || lowerText.contains("xoá") ||
                lowerText.contains("delete") || lowerText.contains("remove")) {
                return handleOfflineDeleteExpense(text);
            }
            // Add expense
            if (containsExpenseKeywords(lowerText)) {
                return handleOfflineAddExpense(text);
            }
        }
        
        return false;
    }
    
    private boolean containsExpenseKeywords(String lowerText) {
        String[] keywords = {"chi tiêu", "mua", "đổ xăng", "ăn", "uống", "cafe", "cà phê", 
                            "nhà hàng", "siêu thị", "shopping", "mỹ phẩm", "quần áo",
                            "điện", "nước", "internet", "điện thoại", "taxi", "grab",
                            "expense", "buy", "gas", "eat", "drink", "coffee",
                            "restaurant", "supermarket", "clothes", "electricity",
                            "water", "phone", "taxi", "grab"};
        for (String keyword : keywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    // ==================== EXPENSE HANDLERS ====================
    
    private boolean handleOfflineAddExpense(String text) {
        try {
            // Extract amount using improved parser
            Long amount = BudgetAmountParser.parseAmount(text);
            if (amount == null) {
                return false;
            }
            
            // Extract date
            Date expenseDate = DateParser.parseDate(text);
            if (expenseDate == null) {
                expenseDate = new Date(); // Default to today
            }
            
            // Extract category (check if any category keyword exists)
            String category = CategoryHelper.detectCategory(text);
            
            // Extract description using proper method
            String description = ExpenseDescriptionParser.extractDescriptionOffline(text, category, amount);
            
            if (description.isEmpty()) {
                description = category; // Use category as description if empty
            }
            
            // Create transaction with proper constructor
            final String finalDesc = description;
            final String finalCategory = category;
            final long finalAmount = amount;
            final long expenseAmount = -Math.abs(amount); // Expense is negative
            final Date finalDate = expenseDate;
            
            TransactionEntity transaction = new TransactionEntity(
                    finalDesc,
                    finalCategory,
                    expenseAmount,
                    finalDate,
                    "expense"
            );
            
            // Save to database
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    AppDatabase.getInstance(context)
                            .transactionDao()
                            .insert(transaction);
                    
                    String formattedAmount = CurrencyFormatter.formatCurrency(context, finalAmount);
                    String successMsg = "✅ Đã thêm chi tiêu (Offline)\n\n" +
                            "📝 " + finalDesc + "\n" +
                            "💰 " + formattedAmount + "\n" +
                            "📂 " + finalCategory;
                    
                    if (callback != null) {
                        callback.onSuccess(successMsg);
                        callback.onToast("Đã thêm: " + finalDesc + " - " + formattedAmount, false);
                        callback.refreshHomeFragment();
                        callback.refreshExpenseWelcomeMessage();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error saving expense", e);
                    if (callback != null) {
                        callback.onError("❌ Lỗi khi thêm chi tiêu: " + e.getMessage());
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error handling offline add expense", e);
            return false;
        }
    }
    
    private boolean handleOfflineDeleteExpense(String text) {
        try {
            // Extract ID from text: "Xóa chi tiêu #123" or "Xóa #123"
            Pattern idPattern = Pattern.compile("#(\\d+)");
            Matcher idMatcher = idPattern.matcher(text);
            
            if (!idMatcher.find()) {
                if (callback != null) {
                    callback.onError("❌ Không tìm thấy ID chi tiêu. Vui lòng sử dụng định dạng: 'Xóa chi tiêu #123'");
                }
                return true;
            }
            
            int id = Integer.parseInt(idMatcher.group(1));
            
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    TransactionEntity transaction = AppDatabase.getInstance(context)
                            .transactionDao()
                            .getTransactionById(id);
                    
                    if (transaction != null) {
                        AppDatabase.getInstance(context)
                                .transactionDao()
                                .delete(transaction);
                        
                        if (callback != null) {
                            callback.onSuccess("✅ Đã xóa chi tiêu #" + id + " (Offline)");
                            callback.onToast("Đã xóa chi tiêu #" + id, false);
                            callback.refreshHomeFragment();
                            callback.refreshExpenseWelcomeMessage();
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("❌ Không tìm thấy chi tiêu #" + id);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting expense", e);
                    if (callback != null) {
                        callback.onError("❌ Lỗi khi xóa chi tiêu: " + e.getMessage());
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error handling offline delete expense", e);
            return false;
        }
    }
    
    // ==================== MONTHLY BUDGET HANDLERS ====================
    
    private boolean handleOfflineUpdateBudget(String text) {
        try {
            String lowerText = text.toLowerCase();
            
            // Extract amount using improved parser
            Long amount = BudgetAmountParser.parseAmount(text);
            if (amount == null) {
                return false;
            }
            
            // Determine if it's absolute or relative change
            boolean isAbsoluteSet = lowerText.contains("lên") || lowerText.contains("xuống");
            boolean isIncrease = lowerText.contains("thêm") || lowerText.contains("nâng") || 
                                lowerText.contains("tăng") || (isAbsoluteSet && lowerText.contains("lên"));
            boolean isDecrease = lowerText.contains("giảm") || lowerText.contains("hạ") || 
                                lowerText.contains("cắt") || lowerText.contains("trừ") || 
                                lowerText.contains("bớt") || (isAbsoluteSet && lowerText.contains("xuống"));
            
            final long finalAmount = amount;
            final boolean finalIsAbsolute = isAbsoluteSet;
            final boolean finalIsIncrease = isIncrease;
            final boolean finalIsDecrease = isDecrease;
            
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    // Get current month
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date startDate = cal.getTime();
                    
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    Date endDate = cal.getTime();
                    
                    List<BudgetEntity> existingBudgets = AppDatabase.getInstance(context)
                            .budgetDao()
                            .getBudgetsByDateRange(startDate, endDate);
                    
                    long newAmount;
                    if (existingBudgets != null && !existingBudgets.isEmpty()) {
                        // Update existing budget
                        BudgetEntity budget = existingBudgets.get(0);
                        long oldAmount = budget.monthlyLimit;
                        
                        if (finalIsAbsolute) {
                            newAmount = finalAmount;
                        } else if (finalIsIncrease) {
                            newAmount = oldAmount + finalAmount;
                        } else if (finalIsDecrease) {
                            newAmount = oldAmount - finalAmount;
                        } else {
                            newAmount = finalAmount; // Default to set
                        }
                        
                        budget.monthlyLimit = newAmount;
                        AppDatabase.getInstance(context)
                                .budgetDao()
                                .update(budget);
                        
                        // Log budget history
                        BudgetHistoryLogger.logMonthlyBudgetUpdated(
                                context, oldAmount, newAmount, startDate);
                    } else {
                        // Create new budget
                        newAmount = finalAmount;
                        BudgetEntity budget = new BudgetEntity(
                                null,           // category (null for monthly budget)
                                newAmount,      // monthlyLimit
                                0,              // currentSpent (start at 0)
                                startDate       // date
                        );
                        AppDatabase.getInstance(context)
                                .budgetDao()
                                .insert(budget);
                        
                        // Log budget history
                        BudgetHistoryLogger.logMonthlyBudgetCreated(
                                context, newAmount, startDate);
                    }
                    
                    String formattedAmount = CurrencyFormatter.formatCurrency(context, newAmount);
                    
                    if (callback != null) {
                        callback.onSuccess("✅ Đã cập nhật ngân sách tháng (Offline)\n\n💰 " + formattedAmount);
                        callback.onToast("Ngân sách đã được cập nhật: " + formattedAmount, false);
                        callback.refreshHomeFragment();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating budget", e);
                    if (callback != null) {
                        callback.onError("❌ Lỗi khi cập nhật ngân sách: " + e.getMessage());
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error handling offline update budget", e);
            return false;
        }
    }
    
    private boolean handleOfflineDeleteBudget(String text) {
        try {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    // Get current month
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date startDate = cal.getTime();
                    
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    Date endDate = cal.getTime();
                    
                    List<BudgetEntity> budgets = AppDatabase.getInstance(context)
                            .budgetDao()
                            .getBudgetsByDateRange(startDate, endDate);
                    
                    if (budgets != null && !budgets.isEmpty()) {
                        BudgetEntity budget = budgets.get(0);
                        long oldAmount = budget.monthlyLimit;
                        
                        AppDatabase.getInstance(context)
                                .budgetDao()
                                .delete(budget);
                        
                        // Log budget history
                        BudgetHistoryLogger.logMonthlyBudgetDeleted(
                                context, oldAmount, startDate);
                        
                        if (callback != null) {
                            callback.onSuccess("✅ Đã xóa ngân sách tháng này (Offline)");
                            callback.onToast("✅ Đã xóa ngân sách tháng", false);
                            callback.refreshHomeFragment();
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("❌ Không tìm thấy ngân sách tháng này để xóa");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting budget", e);
                    if (callback != null) {
                        callback.onError("❌ Lỗi khi xóa ngân sách: " + e.getMessage());
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error handling offline delete budget", e);
            return false;
        }
    }
    
    // ==================== CATEGORY BUDGET HANDLERS ====================
    
    private boolean handleOfflineUpdateCategoryBudget(String text) {
        try {
            String lowerText = text.toLowerCase();
            
            // Extract category
            String category = null;
            for (String cat : CategoryHelper.getAllCategories()) {
                if (lowerText.contains(cat.toLowerCase())) {
                    category = cat;
                    break;
                }
            }
            
            if (category == null) {
                if (callback != null) {
                    callback.onError("❌ Không tìm thấy danh mục. Vui lòng chỉ rõ danh mục (ví dụ: 'ăn uống', 'đi lại')");
                }
                return true;
            }
            
            // Extract amount using improved parser
            Long amount = BudgetAmountParser.parseAmount(text);
            if (amount == null) {
                if (callback != null) {
                    callback.onError("❌ Không tìm thấy số tiền. Vui lòng nhập số tiền (ví dụ: '500k', '2 triệu', '8 tỷ 6')");
                }
                return true;
            }
            
            final String finalCategory = category;
            final long finalAmount = amount;
            
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    // Get current month
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date startDate = cal.getTime();
                    
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    Date endDate = cal.getTime();
                    
                    // Check if category budget exists
                    CategoryBudgetEntity existingBudget =
                            AppDatabase.getInstance(context)
                                    .categoryBudgetDao()
                                    .getCategoryBudgetForMonth(finalCategory, startDate, endDate);
                    
                    if (existingBudget != null) {
                        // Update existing
                        long oldAmount = existingBudget.getBudgetAmount();
                        existingBudget.budgetAmount = finalAmount;  // Direct field access
                        AppDatabase.getInstance(context)
                                .categoryBudgetDao()
                                .update(existingBudget);
                        
                        // Log history
                        BudgetHistoryLogger.logCategoryBudgetUpdated(
                                context, finalCategory, oldAmount, finalAmount);
                    } else {
                        // Create new
                        CategoryBudgetEntity newBudget =
                                new CategoryBudgetEntity(
                                        finalCategory, finalAmount, startDate);
                        AppDatabase.getInstance(context)
                                .categoryBudgetDao()
                                .insert(newBudget);
                        
                        // Log history
                        BudgetHistoryLogger.logCategoryBudgetCreated(
                                context, finalCategory, finalAmount);
                    }
                    
                    String formattedAmount = CurrencyFormatter.formatCurrency(context, finalAmount);
                    
                    if (callback != null) {
                        callback.onSuccess("✅ Đã cập nhật ngân sách danh mục (Offline)\n\n" +
                                "📂 " + finalCategory + "\n💰 " + formattedAmount);
                        callback.onToast("Ngân sách '" + finalCategory + "' đã được cập nhật: " + formattedAmount, false);
                        callback.refreshCategoryBudgetWelcomeMessage();
                        callback.refreshHomeFragment();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating category budget", e);
                    if (callback != null) {
                        callback.onError("❌ Lỗi khi cập nhật ngân sách danh mục: " + e.getMessage());
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error handling offline update category budget", e);
            return false;
        }
    }
    
    private boolean handleOfflineDeleteCategoryBudget(String text) {
        try {
            String lowerText = text.toLowerCase();
            
            // Extract category
            String category = null;
            for (String cat : CategoryHelper.getAllCategories()) {
                if (lowerText.contains(cat.toLowerCase())) {
                    category = cat;
                    break;
                }
            }
            
            if (category == null) {
                if (callback != null) {
                    callback.onError("❌ Không tìm thấy danh mục. Vui lòng chỉ rõ danh mục (ví dụ: 'ăn uống', 'đi lại')");
                }
                return true;
            }
            
            final String finalCategory = category;
            
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    // Get current month
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date startDate = cal.getTime();
                    
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    Date endDate = cal.getTime();
                    
                    CategoryBudgetEntity budget =
                            AppDatabase.getInstance(context)
                                    .categoryBudgetDao()
                                    .getCategoryBudgetForMonth(finalCategory, startDate, endDate);
                    
                    if (budget != null) {
                        long oldAmount = budget.getBudgetAmount();
                        
                        AppDatabase.getInstance(context)
                                .categoryBudgetDao()
                                .delete(budget);
                        
                        // Log history
                        BudgetHistoryLogger.logCategoryBudgetDeleted(
                                context, finalCategory, oldAmount);
                        
                        if (callback != null) {
                            callback.onSuccess("✅ Đã xóa ngân sách danh mục '" + finalCategory + "' (Offline)");
                            callback.onToast("Đã xóa ngân sách danh mục '" + finalCategory + "'", false);
                            callback.refreshCategoryBudgetWelcomeMessage();
                            callback.refreshHomeFragment();
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("❌ Không tìm thấy ngân sách danh mục '" + finalCategory + "' để xóa");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting category budget", e);
                    if (callback != null) {
                        callback.onError("❌ Lỗi khi xóa ngân sách danh mục: " + e.getMessage());
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error handling offline delete category budget", e);
            return false;
        }
    }
}
