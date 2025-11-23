package com.example.spending_management_app.presentation.fragment.home;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.spending_management_app.R;
import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.local.entity.BudgetEntity;
import com.example.spending_management_app.data.local.entity.CategoryBudgetEntity;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet;
import com.example.spending_management_app.utils.CategoryUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

public class BudgetManagementDialog extends DialogFragment {

    private OnActionSelectedListener listener;
    private AppDatabase db;

    public interface OnActionSelectedListener {
        void onAddIncomeSelected();
        void onSetBudgetSelected();
    }

    public void setOnActionSelectedListener(OnActionSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(getContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getContext(), R.style.RoundedDialog4Corners);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_budget_management, null);
        dialog.setContentView(view);

        Button btnAddIncome = view.findViewById(R.id.btn_add_income);
        Button btnSetBudget = view.findViewById(R.id.btn_set_budget);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

    btnAddIncome.setOnClickListener(v -> {
            handleMonthlyBudget();
            dismiss(); // Close dialog
        });

        btnSetBudget.setOnClickListener(v -> {
            android.util.Log.d("BudgetDialog", "Category Budget button clicked");
            handleCategoryBudget();
            // Don't dismiss immediately - let handleCategoryBudget finish first
        });

        btnCancel.setOnClickListener(v -> dismiss());

        return dialog;
    }

    private void handleMonthlyBudget() {
        // Open AI chat bottom sheet with "budget" mode flag
        AiChatBottomSheet aiChatBottomSheet = new AiChatBottomSheet();
        Bundle args = new Bundle();
        args.putString("mode", "budget_management"); // New flag to indicate budget mode
        aiChatBottomSheet.setArguments(args);
        aiChatBottomSheet.show(getParentFragmentManager(), aiChatBottomSheet.getTag());
    }
    
    private void handleCategoryBudget() {
        android.util.Log.d("BudgetDialog", "handleCategoryBudget called");
        
        // Open AI chat bottom sheet with category budget mode and show all category budgets
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                android.util.Log.d("BudgetDialog", "Background thread started");
                
                // Calculate month range
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date startOfMonth = cal.getTime();
                
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                Date endOfMonth = cal.getTime();
                
                android.util.Log.d("BudgetDialog", "Date range: " + startOfMonth + " to " + endOfMonth);
                
                // Get monthly budget for current month
                List<BudgetEntity> monthlyBudgets = db.budgetDao()
                        .getBudgetsByDateRange(startOfMonth, endOfMonth);
                long monthlyBudget = (monthlyBudgets != null && !monthlyBudgets.isEmpty()) 
                        ? monthlyBudgets.get(0).getMonthlyLimit() : 0;
                
                android.util.Log.d("BudgetDialog", "Monthly budget: " + monthlyBudget);
                
                // Get all category budgets for current month
                List<CategoryBudgetEntity> categoryBudgets = db.categoryBudgetDao()
                        .getAllCategoryBudgetsForMonth(startOfMonth, endOfMonth);
                
                android.util.Log.d("BudgetDialog", "Category budgets loaded: " + (categoryBudgets != null ? categoryBudgets.size() : "null"));
                
                // Define all categories in order
                String[] allCategories = {
                    "Ăn uống", "Di chuyển", "Tiện ích", "Y tế", "Nhà ở",
                    "Mua sắm", "Giáo dục", "Sách & Học tập", "Thể thao", "Sức khỏe & Làm đẹp",
                    "Giải trí", "Du lịch", "Ăn ngoài & Cafe", "Quà tặng & Từ thiện", "Hội họp & Tiệc tụng",
                    "Điện thoại & Internet", "Đăng ký & Dịch vụ", "Phần mềm & Apps", "Ngân hàng & Phí",
                    "Con cái", "Thú cưng", "Gia đình",
                    "Lương", "Đầu tư", "Thu nhập phụ", "Tiết kiệm",
                    "Khác"
                };
                
                // Create map of existing budgets for quick lookup
                java.util.Map<String, Long> budgetMap = new java.util.HashMap<>();
                long totalCategoryBudget = 0;
                if (categoryBudgets != null) {
                    for (CategoryBudgetEntity budget : categoryBudgets) {
                        budgetMap.put(budget.getCategory(), budget.getBudgetAmount());
                        totalCategoryBudget += budget.getBudgetAmount();
                    }
                }
                
                // Create list with budgets and amounts (0 for not set)
                List<CategoryBudgetInfo> allCategoryInfo = new java.util.ArrayList<>();
                for (String category : allCategories) {
                    long amount = budgetMap.getOrDefault(category, 0L);
                    allCategoryInfo.add(new CategoryBudgetInfo(category, amount));
                }
                
                // Sort: budgets set (high to low) then unset categories
                allCategoryInfo.sort((a, b) -> {
                    if (a.amount > 0 && b.amount == 0) return -1; // a has budget, b doesn't
                    if (a.amount == 0 && b.amount > 0) return 1;  // b has budget, a doesn't
                    if (a.amount > 0 && b.amount > 0) return Long.compare(b.amount, a.amount); // both have, sort high to low
                    return 0; // both unset, keep order
                });
                
                // Build message to display
                StringBuilder message = new StringBuilder();
                message.append(getString(R.string.category_budget_title));
                
                // Show monthly budget info
                if (monthlyBudget > 0) {
                    message.append(String.format(getString(R.string.monthly_budget_label_short) + " %,d VND\n", monthlyBudget));
                    message.append(String.format(getString(R.string.total_category_budget_label) + " %,d VND\n", totalCategoryBudget));
                    
                    long remaining = monthlyBudget - totalCategoryBudget;
                    if (remaining >= 0) {
                        message.append(String.format(getString(R.string.remaining_budget_label) + " %,d VND\n\n", remaining));
                    } else {
                        message.append(String.format(getString(R.string.exceeded_budget_label) + " %,d VND\n\n", Math.abs(remaining)));
                    }
                } else {
                    message.append(getString(R.string.no_monthly_budget_set));
                }
                
                for (CategoryBudgetInfo info : allCategoryInfo) {
                    String localizedCategory = getLocalizedCategoryName(info.category);
                    if (info.amount > 0) {
                        message.append(String.format("%s: %,d VND\n", 
                                localizedCategory, info.amount));
                    } else {
                        message.append(String.format("%s: %s\n", 
                                localizedCategory, getString(R.string.not_set)));
                    }
                }
                
                message.append(getString(R.string.category_budget_instructions_header));
                message.append(getString(R.string.category_budget_instructions));
                
                String finalMessage = message.toString();
                
                android.util.Log.d("BudgetDialog", "Final message: " + finalMessage);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.util.Log.d("BudgetDialog", "Opening AiChatBottomSheet");
                        
                        // Open AI chat with category budget context
                        AiChatBottomSheet aiChatBottomSheet = new AiChatBottomSheet();
                        Bundle args = new Bundle();
                        args.putString("mode", "category_budget_management");
                        args.putString("welcome_message", finalMessage);
                        aiChatBottomSheet.setArguments(args);
                        aiChatBottomSheet.show(getParentFragmentManager(), aiChatBottomSheet.getTag());
                        
                        android.util.Log.d("BudgetDialog", "AiChatBottomSheet shown");
                        
                        // Dismiss dialog after showing chat
                        dismiss();
                    });
                } else {
                    android.util.Log.e("BudgetDialog", "Activity is null!");
                }
                
            } catch (Exception e) {
                android.util.Log.e("BudgetDialog", "Error loading category budgets", e);
                e.printStackTrace();
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.util.Log.d("BudgetDialog", "Showing default message due to error");
                        
                        // Show error or default message
                        String defaultMessage = getString(R.string.default_category_budget_message);
                        
                        AiChatBottomSheet aiChatBottomSheet = new AiChatBottomSheet();
                        Bundle args = new Bundle();
                        args.putString("mode", "category_budget_management");
                        args.putString("welcome_message", defaultMessage);
                        aiChatBottomSheet.setArguments(args);
                        aiChatBottomSheet.show(getParentFragmentManager(), aiChatBottomSheet.getTag());
                        
                        android.util.Log.d("BudgetDialog", "Default AiChatBottomSheet shown");
                        
                        // Dismiss dialog after showing chat
                        dismiss();
                    });
                } else {
                    android.util.Log.e("BudgetDialog", "Activity is null in error handler!");
                }
            }
        });
    }
    
    private String getLocalizedCategoryName(String category) {
        return CategoryUtils.getLocalizedCategoryName(getContext(), category);
    }
    
    // Helper class to hold category budget information
    private static class CategoryBudgetInfo {
        String category;
        long amount;
        
        CategoryBudgetInfo(String category, long amount) {
            this.category = category;
            this.amount = amount;
        }
    }
}