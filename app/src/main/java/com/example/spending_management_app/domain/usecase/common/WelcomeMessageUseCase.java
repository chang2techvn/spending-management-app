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
import com.example.spending_management_app.utils.CategoryUtils;
import com.example.spending_management_app.utils.CurrencyFormatter;

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
    // Add a temporary loading message (localized)
    messages.add(new ChatMessage(context.getString(com.example.spending_management_app.R.string.loading_budget_info), false, context.getString(com.example.spending_management_app.R.string.now_label)));

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

                // Use device/default locale for month formatting so message localizes properly
                SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());

                // Build welcome message with budget information
                StringBuilder welcomeMessage = new StringBuilder();
                welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.welcome_greeting)).append("\n\n");

                // Check network status and add warning if offline
                if (!isNetworkAvailable(context)) {
                    welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.offline_mode_header)).append("\n");
                    welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.offline_mode_details));
                }

                // Part 1: Budget history from 6 months ago
                if (!pastBudgets.isEmpty()) {
                    welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.budget_history_title)).append("\n\n");

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
                        String formattedAmount = CurrencyFormatter.formatCurrency(context, budget.monthlyLimit);
                        welcomeMessage.append("💰 Tháng ").append(month).append(": ")
                                .append(formattedAmount).append("\n");
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
                    String formattedAmount = CurrencyFormatter.formatCurrency(context, currentBudget.monthlyLimit);
                    String currentMonth = monthFormat.format(currentBudget.date);
                    welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.current_month_budget_label))
                            .append(" (").append(currentMonth).append("): ")
                            .append(formattedAmount).append("\n\n");
                } else {
                    welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.current_month_budget_label))
                            .append(": ").append(context.getString(com.example.spending_management_app.R.string.budget_not_set)).append("\n\n");
                }

                // Part 2: Instructions for managing budget
                welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.budget_instructions)).append("\n");
                welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.example_budget_command));

                String finalMessage = welcomeMessage.toString();

                // Update UI on main thread
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        // Replace loading message with actual welcome message
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(finalMessage, false, context.getString(com.example.spending_management_app.R.string.now_label)));
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
                        fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.welcome_greeting)).append("\n\n");

                        // Check network status and add warning if offline
                        if (!isNetworkAvailable(context)) {
                            fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.offline_mode_header)).append("\n");
                            fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.offline_mode_details));
                        }

                        fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.budget_instructions)).append("\n");
                        fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.example_budget_command));

                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(fallbackMessage.toString(), false, context.getString(com.example.spending_management_app.R.string.now_label)));
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
    // Add a temporary loading message (localized)
    messages.add(new ChatMessage(context.getString(com.example.spending_management_app.R.string.loading), false, context.getString(com.example.spending_management_app.R.string.now_label)));

        // Load recent transactions from database in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<TransactionEntity> recentTransactions = expenseRepository
                        .getRecentTransactions(3);

                // Build welcome message with recent transactions
                StringBuilder welcomeMessage = new StringBuilder();
                welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.welcome_greeting)).append("\n\n");

                // Check network status and add warning if offline
                if (!isNetworkAvailable(context)) {
                    welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.offline_mode_header)).append("\n");
                    welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.offline_mode_details));
                }

                if (!recentTransactions.isEmpty()) {
                    welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.recent_transactions_title)).append("\n\n");

                    for (TransactionEntity transaction : recentTransactions) {
                        String emoji = CategoryHelper.getEmojiForCategory(transaction.category);
                        String formattedAmount = CurrencyFormatter.formatCurrency(context, Math.abs(transaction.amount));
                        String localizedCategory = CategoryUtils.getLocalizedCategoryName(context, transaction.category);
                        welcomeMessage.append(emoji).append(" ")
                                .append(transaction.description).append(": ")
                                .append(formattedAmount)
                                .append(" (").append(localizedCategory).append(")")
                                .append("\n");
                    }
                    welcomeMessage.append("\n");
                }

                welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.add_transaction_instructions)).append("\n");
                welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.example_add_transaction));

                String finalMessage = welcomeMessage.toString();

                // Update UI on main thread
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        // Replace loading message with actual welcome message
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(finalMessage, false, context.getString(com.example.spending_management_app.R.string.now_label)));
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
                        fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.welcome_greeting)).append("\n\n");

                        // Check network status and add warning if offline
                        if (!isNetworkAvailable(context)) {
                            fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.offline_mode_header)).append("\n");
                            fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.offline_mode_details));
                        }

                        fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.add_transaction_instructions)).append("\n");
                        fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.example_add_transaction));

                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(fallbackMessage.toString(), false, context.getString(com.example.spending_management_app.R.string.now_label)));
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
    // Add a temporary loading message (localized)
    messages.add(new ChatMessage(context.getString(com.example.spending_management_app.R.string.loading), false, context.getString(com.example.spending_management_app.R.string.now_label)));

        // Load recent transactions from database in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<TransactionEntity> recentTransactions = expenseRepository
                        .getRecentTransactions(5); // Show 5 recent transactions

                // Build welcome message with recent transactions
                StringBuilder welcomeMessage = new StringBuilder();
                welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.expense_bulk_title)).append("\n\n");

                // Check network status and add warning if offline
                if (!isNetworkAvailable(context)) {
                    welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.offline_mode_header)).append("\n");
                    welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.offline_mode_details));
                }

                if (!recentTransactions.isEmpty()) {
                    welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.expense_bulk_details)).append("\n\n");

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

                    for (TransactionEntity transaction : recentTransactions) {
                        String emoji = CategoryIconHelper.getIconEmoji(transaction.category);
                        String formattedAmount = CurrencyFormatter.formatCurrency(context, Math.abs(transaction.amount));
                        String dateStr = dateFormat.format(transaction.date);

                        welcomeMessage.append(emoji).append(" ")
                                .append(transaction.description)
                                .append(": ").append(formattedAmount)
                                .append(" - ").append(dateStr)
                                .append("\n");
                    }
                    welcomeMessage.append("\n");
                }

                welcomeMessage.append(context.getString(com.example.spending_management_app.R.string.expense_bulk_guidance));

                String finalMessage = welcomeMessage.toString();

                // Update UI on main thread
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        // Replace loading message with actual welcome message
                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(finalMessage, false, context.getString(com.example.spending_management_app.R.string.now_label)));
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
                        fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.expense_bulk_title)).append("\n\n");

                        // Check network status and add warning if offline
                        if (!isNetworkAvailable(context)) {
                            fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.offline_mode_header)).append("\n");
                            fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.offline_mode_details));
                        }

                        fallbackMessage.append(context.getString(com.example.spending_management_app.R.string.expense_bulk_guidance));

                        if (!messages.isEmpty()) {
                            messages.set(0, new ChatMessage(fallbackMessage.toString(), false, context.getString(com.example.spending_management_app.R.string.now_label)));
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