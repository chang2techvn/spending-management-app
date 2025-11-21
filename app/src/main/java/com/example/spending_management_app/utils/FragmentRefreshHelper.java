package com.example.spending_management_app.utils;

import android.app.Activity;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;

import com.example.spending_management_app.MainActivity;
import com.example.spending_management_app.R;
import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.TransactionEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Helper class for refreshing fragments and UI components
 * Handles updates to HomeFragment, HistoryFragment, and welcome messages
 */
public class FragmentRefreshHelper {
    
    private static final String TAG = "FragmentRefreshHelper";
    
    /**
     * Callback interface for fragment refresh operations
     */
    public interface FragmentRefreshCallback {
        void onWelcomeMessageUpdated(String message);
        Activity getActivity();
    }
    
    /**
     * Refresh HomeFragment after transaction changes
     * @param activity The activity containing the fragments
     */
    public static void refreshHomeFragment(Activity activity) {
        try {
            if (activity != null && activity instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) activity;
                // Find HomeFragment and refresh it
                FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
                NavHostFragment navHostFragment = 
                    (NavHostFragment) fragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main);
                
                if (navHostFragment != null) {
                    // Try to find HomeFragment in all child fragments, not just current one
                    FragmentManager childFragmentManager = navHostFragment.getChildFragmentManager();
                    
                    // First try current fragment
                    Fragment currentFragment = childFragmentManager.getPrimaryNavigationFragment();
                    if (currentFragment instanceof com.example.spending_management_app.ui.home.HomeFragment) {
                        com.example.spending_management_app.ui.home.HomeFragment homeFragment = 
                            (com.example.spending_management_app.ui.home.HomeFragment) currentFragment;
                        homeFragment.refreshRecentTransactions();
                        Log.d(TAG, "HomeFragment refreshed (current fragment)");
                        return;
                    }
                    
                    // If not current, search in all fragments
                    for (Fragment fragment : childFragmentManager.getFragments()) {
                        if (fragment instanceof com.example.spending_management_app.ui.home.HomeFragment) {
                            com.example.spending_management_app.ui.home.HomeFragment homeFragment = 
                                (com.example.spending_management_app.ui.home.HomeFragment) fragment;
                            homeFragment.refreshRecentTransactions();
                            Log.d(TAG, "HomeFragment refreshed (found in fragments list)");
                            return;
                        }
                    }
                    
                    Log.d(TAG, "HomeFragment not found in any fragments");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing HomeFragment", e);
        }
    }
    
    /**
     * Refresh HistoryFragment after transaction changes
     * @param activity The activity containing the fragments
     */
    public static void refreshHistoryFragment(Activity activity) {
        try {
            if (activity != null && activity instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) activity;
                // Find HistoryFragment and refresh it
                FragmentManager fragmentManager = mainActivity.getSupportFragmentManager();
                NavHostFragment navHostFragment = 
                    (NavHostFragment) fragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main);
                
                if (navHostFragment != null) {
                    Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                    if (currentFragment instanceof com.example.spending_management_app.ui.history.HistoryFragment) {
                        com.example.spending_management_app.ui.history.HistoryFragment historyFragment = 
                            (com.example.spending_management_app.ui.history.HistoryFragment) currentFragment;
                        historyFragment.refreshTransactions();
                        Log.d(TAG, "HistoryFragment refreshed after transaction save");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing HistoryFragment", e);
        }
    }
    
    /**
     * Refresh expense welcome message with recent transactions
     * @param activity The activity for UI thread operations
     * @param callback Callback to update the welcome message
     */
    public static void refreshExpenseWelcomeMessage(Activity activity, FragmentRefreshCallback callback) {
        // Reload recent transactions and update the first message (welcome message)
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<TransactionEntity> recentTransactions = AppDatabase.getInstance(activity.getApplicationContext())
                        .transactionDao()
                        .getRecentTransactions(5); // Show 5 recent transactions

                // Build updated welcome message
                StringBuilder welcomeMessage = new StringBuilder();
                welcomeMessage.append("📋 Quản lý chi tiêu hàng loạt\n\n");

                if (!recentTransactions.isEmpty()) {
                    welcomeMessage.append("💳 Chi tiêu gần đây:\n\n");

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));

                    for (TransactionEntity transaction : recentTransactions) {
                        String emoji = CategoryIconHelper.getIconEmoji(transaction.category);
                        String formattedAmount = String.format("%,d", Math.abs(transaction.amount));
                        String dateStr = dateFormat.format(transaction.date);

                        welcomeMessage.append(emoji).append(" ")
                                .append(transaction.description)
                                .append(": ").append(formattedAmount).append(" VND")
                                .append(" - ").append(dateStr)
                                .append("\n");
                    }
                    welcomeMessage.append("\n");
                }

                welcomeMessage.append("💡 Hướng dẫn:\n");
                welcomeMessage.append("• Thêm: 'Hôm qua ăn sáng 25k và cafe 30k'\n");
                welcomeMessage.append("• Xóa: 'Xóa chi tiêu #123' (tìm ID ở trang Lịch sử)");

                String finalMessage = welcomeMessage.toString();

                if (activity != null && callback != null) {
                    activity.runOnUiThread(() -> {
                        callback.onWelcomeMessageUpdated(finalMessage);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error refreshing expense welcome message", e);
            }
        });
    }
    
    /**
     * Refresh category budget welcome message with current budget data
     * @param activity The activity for UI thread operations
     * @param callback Callback to update the welcome message
     */
    public static void refreshCategoryBudgetWelcomeMessage(Activity activity, FragmentRefreshCallback callback) {
        // Refresh the first message (welcome message) with updated category budget data
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get current month range
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                java.util.Date startOfMonth = cal.getTime();
                
                cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                cal.set(java.util.Calendar.MINUTE, 59);
                cal.set(java.util.Calendar.SECOND, 59);
                cal.set(java.util.Calendar.MILLISECOND, 999);
                java.util.Date endOfMonth = cal.getTime();
                
                // Get monthly budget for current month
                List<com.example.spending_management_app.database.BudgetEntity> monthlyBudgets = 
                        AppDatabase.getInstance(activity.getApplicationContext()).budgetDao()
                                .getBudgetsByDateRange(startOfMonth, endOfMonth);
                long monthlyBudget = (monthlyBudgets != null && !monthlyBudgets.isEmpty()) 
                        ? monthlyBudgets.get(0).getMonthlyLimit() : 0;
                
                // Get all category budgets for current month
                List<com.example.spending_management_app.database.CategoryBudgetEntity> categoryBudgets = 
                        AppDatabase.getInstance(activity.getApplicationContext())
                                .categoryBudgetDao()
                                .getAllCategoryBudgetsForMonth(startOfMonth, endOfMonth);
                
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
                
                // Create map of existing budgets
                java.util.Map<String, Long> budgetMap = new java.util.HashMap<>();
                long totalCategoryBudget = 0;
                if (categoryBudgets != null) {
                    for (com.example.spending_management_app.database.CategoryBudgetEntity budget : categoryBudgets) {
                        budgetMap.put(budget.getCategory(), budget.getBudgetAmount());
                        totalCategoryBudget += budget.getBudgetAmount();
                    }
                }
                
                // Create list with budgets and amounts
                class CategoryInfo {
                    String category;
                    long amount;
                    CategoryInfo(String category, long amount) {
                        this.category = category;
                        this.amount = amount;
                    }
                }
                
                java.util.List<CategoryInfo> allCategoryInfo = new java.util.ArrayList<>();
                for (String category : allCategories) {
                    long amount = budgetMap.getOrDefault(category, 0L);
                    allCategoryInfo.add(new CategoryInfo(category, amount));
                }
                
                // Sort: budgets set (high to low) then unset categories
                allCategoryInfo.sort((a, b) -> {
                    if (a.amount > 0 && b.amount == 0) return -1;
                    if (a.amount == 0 && b.amount > 0) return 1;
                    if (a.amount > 0 && b.amount > 0) return Long.compare(b.amount, a.amount);
                    return 0;
                });
                
                // Build updated message
                StringBuilder message = new StringBuilder();
                message.append("📊 Ngân sách theo danh mục hiện tại:\n\n");
                
                // Show monthly budget info
                if (monthlyBudget > 0) {
                    message.append(String.format("💰 Ngân sách tháng: %,d VND\n", monthlyBudget));
                    message.append(String.format("📈 Tổng ngân sách danh mục: %,d VND\n", totalCategoryBudget));
                    
                    long remaining = monthlyBudget - totalCategoryBudget;
                    if (remaining >= 0) {
                        message.append(String.format("✅ Còn lại: %,d VND\n\n", remaining));
                    } else {
                        message.append(String.format("⚠️ Vượt quá: %,d VND\n\n", Math.abs(remaining)));
                    }
                } else {
                    message.append("⚠️ Chưa thiết lập ngân sách tháng\n");
                    message.append("💡 Hãy thêm ngân sách tháng trước!\n\n");
                }
                
                for (CategoryInfo info : allCategoryInfo) {
                    String icon = CategoryIconHelper.getIconEmoji(info.category);
                    if (info.amount > 0) {
                        message.append(String.format("%s %s: %,d VND\n", 
                                icon, info.category, info.amount));
                    } else {
                        message.append(String.format("%s %s: Chưa thiết lập\n", 
                                icon, info.category));
                    }
                }
                
                message.append("\n💡 Hướng dẫn:\n");
                message.append("        • Thêm: 'Thêm 500 ngàn ăn uống và 300 ngàn di chuyển'\n");
                message.append("        • Sửa: 'Sửa ăn uống 700 ngàn, mua sắm 400 ngàn'\n");
                message.append("        • Xóa: 'Xóa ngân sách ăn uống và di chuyển'\n");
                message.append("\n⚠️ Lưu ý: Tổng ngân sách danh mục không vượt quá ngân sách tháng");
                
                String finalMessage = message.toString();
                
                if (activity != null && callback != null) {
                    activity.runOnUiThread(() -> {
                        callback.onWelcomeMessageUpdated(finalMessage);
                    });
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing category budget welcome message", e);
            }
        });
    }
}
