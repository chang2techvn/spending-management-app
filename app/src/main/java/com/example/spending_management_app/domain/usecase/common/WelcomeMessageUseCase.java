package com.example.spending_management_app.domain.usecase.common;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import androidx.recyclerview.widget.RecyclerView;
import com.example.spending_management_app.domain.repository.BudgetRepository;
import com.example.spending_management_app.domain.repository.ExpenseRepository;
import com.example.spending_management_app.data.local.entity.BudgetEntity;
import com.example.spending_management_app.data.local.entity.TransactionEntity;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet.ChatAdapter;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet.ChatMessage;
import com.example.spending_management_app.utils.CategoryHelper;
import com.example.spending_management_app.utils.CategoryIconHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Service class for handling welcome message loading operations
 * Extracted from AiChatBottomSheet.java to reduce file size and improve maintainability
 */
public class WelcomeMessageUseCase {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;

    public WelcomeMessageUseCase(BudgetRepository budgetRepository, ExpenseRepository expenseRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Load budget welcome message with budget history and current budget information
     */
    public void loadBudgetWelcomeMessage(Context context, Activity activity,
            List<ChatMessage> messages, ChatAdapter chatAdapter, RecyclerView messagesRecycler,
            Runnable refreshHomeFragment) {
        // Add a temporary loading message
        messages.add(new ChatMessage("Đang tải thông tin ngân sách...", false, "Bây giờ"));

        // Load budget data from database in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get current month's budget
                Calendar currentCal = Calendar.getInstance();
                currentCal.set(Calendar.DAY_OF_MONTH, 1);
                currentCal.set(Calendar.HOUR_OF_DAY, 0);
                currentCal.set(Calendar.MINUTE, 0);
                currentCal.set(Calendar.SECOND, 0);
                currentCal.set(Calendar.MILLISECOND, 0);
                Date currentMonthStart = currentCal.getTime();

                currentCal.set(Calendar.DAY_OF_MONTH, currentCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                currentCal.set(Calendar.HOUR_OF_DAY, 23);
                currentCal.set(Calendar.MINUTE, 59);
                currentCal.set(Calendar.SECOND, 59);
                currentCal.set(Calendar.MILLISECOND, 999);
                Date currentMonthEnd = currentCal.getTime();

                Log.d("WelcomeMessageService", "Loading budget for range: " + currentMonthStart + " to " + currentMonthEnd);

                List<BudgetEntity> currentMonthBudgets = budgetRepository
                        .getBudgetsByDateRangeOrdered(currentMonthStart, currentMonthEnd);

                Log.d("WelcomeMessageService", "Found " + (currentMonthBudgets != null ? currentMonthBudgets.size() : 0) + " budgets for current month");

                // Get budgets from 6 months ago
                Calendar pastCal = Calendar.getInstance();
                pastCal.add(Calendar.MONTH, -6);
                pastCal.set(Calendar.DAY_OF_MONTH, 1);
                Date sixMonthsAgoStart = pastCal.getTime();

                List<BudgetEntity> pastBudgets = budgetRepository
                        .getBudgetsByDateRangeOrdered(sixMonthsAgoStart, currentMonthEnd);

                SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));

                // Build welcome message with budget information
                StringBuilder welcomeMessage = new StringBuilder();
                welcomeMessage.append("Chào bạn! 👋\n\n");

                // Check network status and add warning if offline
                if (!isNetworkAvailable(context)) {
                    welcomeMessage.append("⚠️ CHẾ ĐỘ OFFLINE\n");
                    welcomeMessage.append("Bạn có thể:\n");
                    welcomeMessage.append("✅ Thêm, sửa, xóa chi tiêu\n");
                    welcomeMessage.append("✅ Quản lý ngân sách\n");
                    welcomeMessage.append("❌ Không thể phân tích và tư vấn với AI\n\n");
                }

                // Part 1: Budget history from 6 months ago
                if (!pastBudgets.isEmpty()) {
                    welcomeMessage.append("📊 Ngân sách 6 tháng gần đây:\n\n");

                    // Group budgets by month and show the most recent one for each month
                    java.util.Map<String, BudgetEntity> budgetsByMonth = new java.util.HashMap<>();
                    for (BudgetEntity budget : pastBudgets) {
                        String monthKey = monthFormat.format(budget.date);
                        if (!budgetsByMonth.containsKey(monthKey) ||
                            budget.date.after(budgetsByMonth.get(monthKey).date)) {
                            budgetsByMonth.put(monthKey, budget);
                        }
                    }

                    // Sort and display (limit to last 6 entries)
                    java.util.List<String> sortedMonths = new java.util.ArrayList<>(budgetsByMonth.keySet());
                    java.util.Collections.sort(sortedMonths);

                    // Only show last 6 entries
                    int startIndex = Math.max(0, sortedMonths.size() - 6);
                    for (int i = startIndex; i < sortedMonths.size(); i++) {
                        String month = sortedMonths.get(i);
                        BudgetEntity budget = budgetsByMonth.get(month);
                        String formattedAmount = String.format("%,d", budget.monthlyLimit);
                        welcomeMessage.append("💰 Tháng ").append(month).append(": ")
                                .append(formattedAmount).append(" VND\n");
                    }
                    welcomeMessage.append("\n");
                }

                // Current month budget
                Log.d("WelcomeMessageService", "Current month budgets found: " + (currentMonthBudgets != null ? currentMonthBudgets.size() : 0));
                if (currentMonthBudgets != null) {
                    for (int i = 0; i < currentMonthBudgets.size(); i++) {
                        BudgetEntity b = currentMonthBudgets.get(i);
                        Log.d("WelcomeMessageService", "Budget " + i + ": date=" + b.date + ", amount=" + b.monthlyLimit);
                    }
                }

                if (!currentMonthBudgets.isEmpty()) {
                    BudgetEntity currentBudget = currentMonthBudgets.get(0);
                    String formattedAmount = String.format("%,d", currentBudget.monthlyLimit);
                    String currentMonth = monthFormat.format(currentBudget.date);
                    welcomeMessage.append("📅 Ngân sách tháng này (").append(currentMonth).append("): ")
                            .append(formattedAmount).append(" VND\n\n");
                } else {
                    welcomeMessage.append("📅 Ngân sách tháng này: Chưa thiết lập\n\n");
                }

                // Part 2: Instructions for managing budget
                welcomeMessage.append("💡 Để quản lý ngân sách, hãy cho tôi biết:\n");
                welcomeMessage.append("Ví dụ: \"Thêm ngân sách 15 triệu\" hoặc \"Sửa ngân sách lên 20 triệu\"");

                String finalMessage = welcomeMessage.toString();

                // Update UI on main thread
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        // Replace loading message with actual welcome message
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(finalMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }

            } catch (Exception e) {
                Log.e("WelcomeMessageService", "Error loading budget information", e);

                // Fallback to simple welcome message
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        StringBuilder fallbackMessage = new StringBuilder();
                        fallbackMessage.append("Chào bạn! 👋\n\n");

                        // Check network status and add warning if offline
                        if (!isNetworkAvailable(context)) {
                            fallbackMessage.append("⚠️ CHẾ ĐỘ OFFLINE\n");
                            fallbackMessage.append("Bạn có thể:\n");
                            fallbackMessage.append("✅ Thêm, sửa, xóa chi tiêu\n");
                            fallbackMessage.append("✅ Quản lý ngân sách\n");
                            fallbackMessage.append("❌ Không thể phân tích và tư vấn với AI\n\n");
                        }

                        fallbackMessage.append("💡 Để quản lý ngân sách tháng, hãy cho tôi biết:\n");
                        fallbackMessage.append("Ví dụ: \"Đặt ngân sách 15 triệu\" hoặc \"Sửa ngân sách lên 20 triệu\"");

                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(fallbackMessage.toString(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }
            }
        });
    }

    /**
     * Load recent transactions welcome message for expense tracking
     */
    public void loadRecentTransactionsForWelcome(Context context, Activity activity,
            List<ChatMessage> messages, ChatAdapter chatAdapter, RecyclerView messagesRecycler) {
        // Add a temporary loading message
        messages.add(new ChatMessage("Đang tải...", false, "Bây giờ"));

        // Load recent transactions from database in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<TransactionEntity> recentTransactions = expenseRepository
                        .getRecentTransactions(3);

                // Build welcome message with recent transactions
                StringBuilder welcomeMessage = new StringBuilder();
                welcomeMessage.append("Chào bạn! 👋\n\n");

                // Check network status and add warning if offline
                if (!isNetworkAvailable(context)) {
                    welcomeMessage.append("⚠️ CHẾ ĐỘ OFFLINE\n");
                    welcomeMessage.append("Bạn có thể:\n");
                    welcomeMessage.append("✅ Thêm, sửa, xóa chi tiêu\n");
                    welcomeMessage.append("✅ Quản lý ngân sách\n");
                    welcomeMessage.append("❌ Không thể phân tích và tư vấn với AI\n\n");
                }

                if (!recentTransactions.isEmpty()) {
                    welcomeMessage.append("📋 Chi tiêu gần đây:\n\n");

                    for (TransactionEntity transaction : recentTransactions) {
                        String emoji = CategoryHelper.getEmojiForCategory(transaction.category);
                        String formattedAmount = String.format("%,d", Math.abs(transaction.amount));
                        welcomeMessage.append(emoji).append(" ")
                                .append(transaction.description).append(": ")
                                .append(formattedAmount).append(" VND")
                                .append(" (").append(transaction.category).append(")")
                                .append("\n");
                    }
                    welcomeMessage.append("\n");
                }

                welcomeMessage.append("💡 Để thêm chi tiêu mới, hãy cho tôi biết:\n");
                welcomeMessage.append("Ví dụ: \"Hôm qua tôi đổ xăng 50k\" hoặc \"Ngày 10/11 mua cafe 25k\"");

                String finalMessage = welcomeMessage.toString();

                // Update UI on main thread
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        // Replace loading message with actual welcome message
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(finalMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }

            } catch (Exception e) {
                Log.e("WelcomeMessageService", "Error loading recent transactions", e);

                // Fallback to simple welcome message
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        StringBuilder fallbackMessage = new StringBuilder();
                        fallbackMessage.append("Chào bạn! 👋\n\n");

                        // Check network status and add warning if offline
                        if (!isNetworkAvailable(context)) {
                            fallbackMessage.append("⚠️ CHẾ ĐỘ OFFLINE\n");
                            fallbackMessage.append("Bạn có thể:\n");
                            fallbackMessage.append("✅ Thêm, sửa, xóa chi tiêu\n");
                            fallbackMessage.append("✅ Quản lý ngân sách\n");
                            fallbackMessage.append("❌ Không thể phân tích và tư vấn với AI\n\n");
                        }

                        fallbackMessage.append("💡 Để thêm chi tiêu mới, hãy cho tôi biết:\n");
                        fallbackMessage.append("Ví dụ: \"Hôm qua tôi đổ xăng 50k\" hoặc \"Ngày 10/11 mua cafe 25k\"");

                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(fallbackMessage.toString(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }
            }
        });
    }

    /**
     * Load expense bulk welcome message for bulk expense management
     */
    public void loadExpenseBulkWelcomeMessage(Context context, Activity activity,
            List<ChatMessage> messages, ChatAdapter chatAdapter, RecyclerView messagesRecycler,
            Runnable refreshHomeFragment, Runnable refreshExpenseWelcomeMessage) {
        // Add a temporary loading message
        messages.add(new ChatMessage("Đang tải...", false, "Bây giờ"));

        // Load recent transactions from database in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<TransactionEntity> recentTransactions = expenseRepository
                        .getRecentTransactions(5); // Show 5 recent transactions

                // Build welcome message with recent transactions
                StringBuilder welcomeMessage = new StringBuilder();
                welcomeMessage.append("📋 Quản lý chi tiêu hàng loạt\n\n");

                // Check network status and add warning if offline
                if (!isNetworkAvailable(context)) {
                    welcomeMessage.append("⚠️ CHẾ ĐỘ OFFLINE\n");
                    welcomeMessage.append("Bạn có thể:\n");
                    welcomeMessage.append("✅ Thêm, sửa, xóa chi tiêu\n");
                    welcomeMessage.append("✅ Quản lý ngân sách\n");
                    welcomeMessage.append("❌ Không thể phân tích và tư vấn với AI\n\n");
                }

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

                // Update UI on main thread
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        // Replace loading message with actual welcome message
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(finalMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }

            } catch (Exception e) {
                Log.e("WelcomeMessageService", "Error loading expense bulk welcome message", e);

                // Fallback to simple welcome message
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        StringBuilder fallbackMessage = new StringBuilder();
                        fallbackMessage.append("📋 Quản lý chi tiêu hàng loạt\n\n");

                        // Check network status and add warning if offline
                        if (!isNetworkAvailable(context)) {
                            fallbackMessage.append("⚠️ CHẾ ĐỘ OFFLINE\n");
                            fallbackMessage.append("Bạn có thể:\n");
                            fallbackMessage.append("✅ Thêm, sửa, xóa chi tiêu\n");
                            fallbackMessage.append("✅ Quản lý ngân sách\n");
                            fallbackMessage.append("❌ Không thể phân tích và tư vấn với AI\n\n");
                        }

                        fallbackMessage.append("💡 Hướng dẫn:\n");
                        fallbackMessage.append("• Thêm: 'Hôm qua ăn sáng 25k và cafe 30k'\n");
                        fallbackMessage.append("• Xóa: 'Xóa chi tiêu #123' (tìm ID ở trang Lịch sử)");

                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(fallbackMessage.toString(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(0);
                        }
                    });
                }
            }
        });
    }

    /**
     * Check if network is available
     */
    private static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }

        android.net.ConnectivityManager connectivityManager =
            (android.net.ConnectivityManager) context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.net.Network network = connectivityManager.getActiveNetwork();
                if (network == null) return false;

                android.net.NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(network);

                return capabilities != null && (
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                );
            } else {
                android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        }

        return false;
    }
}