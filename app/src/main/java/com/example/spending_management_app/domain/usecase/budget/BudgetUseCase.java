package com.example.spending_management_app.domain.usecase.budget;

import android.app.Activity;
import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.R;
import com.example.spending_management_app.data.local.entity.BudgetEntity;
import com.example.spending_management_app.domain.repository.BudgetRepository;
import com.example.spending_management_app.domain.usecase.ai.AiContextUseCase;
import com.example.spending_management_app.domain.usecase.ai.PromptUseCase;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet;
import com.example.spending_management_app.utils.BudgetAmountParser;
import com.example.spending_management_app.utils.DateParser;
import com.example.spending_management_app.utils.ToastHelper;
import com.example.spending_management_app.utils.CurrencyFormatter;
import com.example.spending_management_app.utils.UserSession;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Service class for handling budget management operations
 */
public class BudgetUseCase {

    private final BudgetRepository budgetRepository;
    private final PromptUseCase promptUseCase;
    private final AiContextUseCase aiContextUseCase;
    private final UserSession userSession;

    public BudgetUseCase(BudgetRepository budgetRepository, PromptUseCase promptUseCase, AiContextUseCase aiContextUseCase, Context context) {
        this.budgetRepository = budgetRepository;
        this.promptUseCase = promptUseCase;
        this.aiContextUseCase = aiContextUseCase;
        this.userSession = UserSession.getInstance(context);
    }

    /**
     * Handle budget request (add, edit, increase, decrease budget)
     */
    public void handleBudgetRequest(String text, Context context, Activity activity,
                                         List<AiChatBottomSheet.ChatMessage> messages,
                                         AiChatBottomSheet.ChatAdapter chatAdapter,
                                         RecyclerView messagesRecycler,
                                         Runnable refreshHomeFragmentCallback) {
        // Add analyzing message
        int analyzingIndex = messages.size();
        messages.add(new AiChatBottomSheet.ChatMessage(context.getString(R.string.processing_request), false, context.getString(R.string.now_label)));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        // Check if this is an increase/decrease request or absolute set request
        String textLower = text.toLowerCase().trim();

        android.util.Log.d("BudgetService", "=== BUDGET REQUEST DEBUG ===");
        android.util.Log.d("BudgetService", "Original text: [" + text + "]");
        android.util.Log.d("BudgetService", "Lowercase text: [" + textLower + "]");

        // Check for ABSOLUTE set commands with "lên" or "xuống" (Vietnamese)
        // "Tăng lên 10 triệu", "Nâng lên 10 triệu", "Hạ xuống 10 triệu", "Giảm xuống 10 triệu"
        boolean hasLenKeyword = textLower.contains("lên");
        boolean hasXuongKeyword = textLower.contains("xuống");
        boolean isAbsoluteSetVietnamese = ((textLower.contains("tăng") || textLower.contains("nâng")) && hasLenKeyword) ||
                                ((textLower.contains("giảm") || textLower.contains("hạ")) && hasXuongKeyword);

        // Check for ABSOLUTE set commands (English)
        // "Set to 10 million", "Increase to 10 million", "Decrease to 10 million", "Raise to 10 million", "Lower to 10 million"
        boolean hasToKeyword = textLower.contains(" to ");
        boolean isAbsoluteSetEnglish = ((textLower.contains("set") || textLower.contains("put") ||
                                        textLower.contains("increase") || textLower.contains("raise") ||
                                        textLower.contains("change")) && hasToKeyword) ||
                                      ((textLower.contains("decrease") || textLower.contains("lower") ||
                                        textLower.contains("reduce")) && hasToKeyword);

        boolean isAbsoluteSet = isAbsoluteSetVietnamese || isAbsoluteSetEnglish;

        android.util.Log.d("BudgetService", "Has 'lên': " + hasLenKeyword + ", Has 'xuống': " + hasXuongKeyword);
        android.util.Log.d("BudgetService", "Has 'to': " + hasToKeyword);
        android.util.Log.d("BudgetService", "isAbsoluteSet: " + isAbsoluteSet);

        // Check for RELATIVE increase (add more) - only if NOT absolute set (Vietnamese)
        // "Nâng ngân sách 10 triệu", "Tăng ngân sách 10 triệu", "Tăng thêm 10 triệu"
        boolean hasIncreaseKeywordVietnamese = textLower.contains("nâng") ||
                                     textLower.contains("tăng") ||
                                     textLower.contains("cộng") ||
                                     textLower.contains("thêm");

        // Check for RELATIVE increase (add more) - only if NOT absolute set (English)
        // "Increase budget by 10 million", "Add 10 million", "Raise budget 10 million"
        boolean hasIncreaseKeywordEnglish = textLower.contains("increase") ||
                                           textLower.contains("add") ||
                                           textLower.contains("raise") ||
                                           textLower.contains("plus");

        boolean hasIncreaseKeyword = hasIncreaseKeywordVietnamese || hasIncreaseKeywordEnglish;
        boolean isIncrease = !isAbsoluteSet && hasIncreaseKeyword;

        android.util.Log.d("BudgetService", "Has increase keyword: " + hasIncreaseKeyword + ", isIncrease: " + isIncrease);

        // Check for RELATIVE decrease (subtract) - only if NOT absolute set (Vietnamese)
        // "Giảm ngân sách 2 triệu", "Hạ ngân sách 1 triệu", "Trừ 2 triệu"
        boolean hasDecreaseKeywordVietnamese = textLower.contains("giảm") ||
                                     textLower.contains("hạ") ||
                                     textLower.contains("trừ") ||
                                     textLower.contains("bớt") ||
                                     textLower.contains("cắt");

        // Check for RELATIVE decrease (subtract) - only if NOT absolute set (English)
        // "Decrease budget by 2 million", "Reduce budget 1 million", "Subtract 2 million"
        boolean hasDecreaseKeywordEnglish = textLower.contains("decrease") ||
                                           textLower.contains("reduce") ||
                                           textLower.contains("lower") ||
                                           textLower.contains("subtract") ||
                                           textLower.contains("minus") ||
                                           textLower.contains("cut");

        boolean hasDecreaseKeyword = hasDecreaseKeywordVietnamese || hasDecreaseKeywordEnglish;
        boolean isDecrease = !isAbsoluteSet && hasDecreaseKeyword;

        android.util.Log.d("BudgetService", "Has decrease keyword: " + hasDecreaseKeyword + ", isDecrease: " + isDecrease);
        android.util.Log.d("BudgetService", "=== FINAL RESULT: isAbsoluteSet=" + isAbsoluteSet + ", isIncrease=" + isIncrease + ", isDecrease=" + isDecrease + " ===");

        // Extract amount from text (support various formats like "15 triệu", "20000000", "25tr")
        long amount = BudgetAmountParser.extractBudgetAmount(text);

        // Extract month and year from text (default to current month if not specified)
        int[] monthYear = DateParser.extractMonthYear(text);
        int targetMonth = monthYear[0];
        int targetYear = monthYear[1];

        // Get current month and year for validation
        Calendar currentCal = Calendar.getInstance();
        int currentMonth = currentCal.get(Calendar.MONTH) + 1; // 0-based to 1-based
        int currentYear = currentCal.get(Calendar.YEAR);

        // Validate: only allow current month and future months
        if (targetYear < currentYear || (targetYear == currentYear && targetMonth < currentMonth)) {
            activity.runOnUiThread(() -> {
                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                        String.format(context.getString(R.string.budget_past_month_error), currentMonth, currentYear),
                        false, context.getString(R.string.now_label)));
                chatAdapter.notifyItemChanged(analyzingIndex);
            });
            return;
        }

        if (amount > 0) {
            // Save budget to database
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    // Create calendar for target month
                    Calendar targetCal = Calendar.getInstance();
                    targetCal.set(Calendar.YEAR, targetYear);
                    targetCal.set(Calendar.MONTH, targetMonth - 1); // 1-based to 0-based
                    targetCal.set(Calendar.DAY_OF_MONTH, 1);
                    targetCal.set(Calendar.HOUR_OF_DAY, 0);
                    targetCal.set(Calendar.MINUTE, 0);
                    targetCal.set(Calendar.SECOND, 0);
                    targetCal.set(Calendar.MILLISECOND, 0);
                    Date startOfMonth = targetCal.getTime();

                    targetCal.set(Calendar.DAY_OF_MONTH, targetCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    targetCal.set(Calendar.HOUR_OF_DAY, 23);
                    targetCal.set(Calendar.MINUTE, 59);
                    targetCal.set(Calendar.SECOND, 59);
                    Date endOfMonth = targetCal.getTime();

                    android.util.Log.d("BudgetService", "Saving budget for range: " + startOfMonth + " to " + endOfMonth);

                    int userId = userSession.getCurrentUserId();
                    List<BudgetEntity> existingBudgets = budgetRepository
                            .getBudgetsByDateRangeOrdered(userId, startOfMonth, endOfMonth);

                    android.util.Log.d("BudgetService", "Found " + (existingBudgets != null ? existingBudgets.size() : 0) + " existing budgets for userId: " + userId);

                    boolean isUpdate = !existingBudgets.isEmpty();

                    // Use the first day of target month as the budget date
                    Date budgetDate = startOfMonth;

                    android.util.Log.d("BudgetService", "Budget date to save: " + budgetDate + ", Amount: " + amount + ", isAbsoluteSet: " + isAbsoluteSet + ", isIncrease: " + isIncrease + ", isDecrease: " + isDecrease);

                    // Calculate final amount and determine action type
                    long calculatedFinalAmount;
                    String determinedActionType;

                    if (isUpdate) {
                        // Update existing budget
                        BudgetEntity existing = existingBudgets.get(0);
                        android.util.Log.d("BudgetService", "Updating existing budget, old date: " + existing.date + ", new date: " + budgetDate);
                        long oldAmount = existing.monthlyLimit;

                        // Calculate final amount based on operation type
                        if (isAbsoluteSet) {
                            // Absolute set: "Tăng lên 10 triệu", "Giảm xuống 10 triệu" -> Set to exact amount
                            calculatedFinalAmount = amount;
                            determinedActionType = "set";
                            android.util.Log.d("BudgetService", "Setting budget to absolute value: " + calculatedFinalAmount);
                        } else if (isIncrease) {
                            // Relative increase: "Nâng 10 triệu", "Tăng thêm 10 triệu" -> Add amount
                            calculatedFinalAmount = oldAmount + amount;
                            determinedActionType = "increase";
                            android.util.Log.d("BudgetService", "Increasing budget: " + oldAmount + " + " + amount + " = " + calculatedFinalAmount);
                        } else if (isDecrease) {
                            // Relative decrease: "Giảm 2 triệu", "Trừ 2 triệu" -> Subtract amount
                            long tempAmount = oldAmount - amount;
                            // Don't allow negative budget
                            if (tempAmount < 0) {
                                android.util.Log.w("BudgetService", "Final amount would be negative, setting to 0");
                                calculatedFinalAmount = 0;
                            } else {
                                calculatedFinalAmount = tempAmount;
                            }
                            determinedActionType = "decrease";
                            android.util.Log.d("BudgetService", "Decreasing budget: " + oldAmount + " - " + amount + " = " + calculatedFinalAmount);
                        } else {
                            // Default: Set to amount (backward compatibility)
                            calculatedFinalAmount = amount;
                            determinedActionType = "set";
                        }

                        existing.monthlyLimit = calculatedFinalAmount;
                        existing.date = budgetDate;
                        budgetRepository.update(existing);

                        // Log budget history
                        BudgetHistoryLogger.logMonthlyBudgetUpdated(
                                context, oldAmount, calculatedFinalAmount, budgetDate);
                    } else {
                        // Insert new budget - ignore increase/decrease for new budget
                        if (isIncrease || isDecrease) {
                            // No existing budget to increase/decrease
                            if (activity != null) {
                                activity.runOnUiThread(() -> {
                                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
                                    String monthYearStr = monthYearFormat.format(budgetDate);
                                    messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                                            String.format(context.getString(R.string.budget_not_exists_for_operation), 
                                                    monthYearStr, (isIncrease ? context.getString(R.string.increase) : context.getString(R.string.decrease)), monthYearStr),
                                            false, context.getString(R.string.now_label)));
                                    chatAdapter.notifyItemChanged(analyzingIndex);
                                });
                            }
                            return; // Exit without creating new budget
                        }

                        calculatedFinalAmount = amount;
                        determinedActionType = "set";

                        BudgetEntity budget = new BudgetEntity("Ngân sách tháng", calculatedFinalAmount, 0L, budgetDate);
                        budget.setUserId(userSession.getCurrentUserId());
                        android.util.Log.d("BudgetService", "Inserting new budget: " + budget.date + " for userId: " + budget.getUserId());
                        budgetRepository.insert(budget);

                        // Log budget history
                        BudgetHistoryLogger.logMonthlyBudgetCreated(
                                context, calculatedFinalAmount, budgetDate);
                    }

                    // Make final variables for lambda
                    final long finalAmount = calculatedFinalAmount;
                    final String actionType = determinedActionType;

                    String formattedFinalAmount = CurrencyFormatter.formatCurrency(context, finalAmount);
                    String formattedChangeAmount = CurrencyFormatter.formatCurrency(context, amount);
                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
                    String monthYearStr = monthYearFormat.format(budgetDate);

                    // Update UI
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            String responseMessage;
                            String toastMessage;

                            if (isUpdate) {
                                if (actionType.equals("increase")) {
                                    responseMessage = String.format(context.getString(R.string.budget_increased_success), monthYearStr, formattedChangeAmount, formattedFinalAmount);
                                    toastMessage = String.format(context.getString(R.string.budget_increased_toast), monthYearStr, formattedChangeAmount);
                                } else if (actionType.equals("decrease")) {
                                    responseMessage = String.format(context.getString(R.string.budget_decreased_success), monthYearStr, formattedChangeAmount, formattedFinalAmount);
                                    toastMessage = String.format(context.getString(R.string.budget_decreased_toast), monthYearStr, formattedChangeAmount);
                                } else {
                                    responseMessage = String.format(context.getString(R.string.budget_updated_success), monthYearStr, formattedFinalAmount);
                                    toastMessage = String.format(context.getString(R.string.budget_updated_toast), monthYearStr, formattedFinalAmount);
                                }
                            } else {
                                responseMessage = String.format(context.getString(R.string.budget_set_success), monthYearStr, formattedFinalAmount);
                                toastMessage = String.format(context.getString(R.string.budget_set_toast), monthYearStr, formattedFinalAmount);
                            }

                            messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(responseMessage, false, context.getString(R.string.now_label)));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);

                            ToastHelper.showToastOnTop(activity, toastMessage);

                            // Refresh HomeFragment
                            refreshHomeFragmentCallback.run();
                        });
                    }

                } catch (Exception e) {
                    android.util.Log.e("BudgetService", "Error saving budget", e);

                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                                    context.getString(R.string.budget_save_error_message),
                                    false, context.getString(R.string.now_label)));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            ToastHelper.showErrorToast(activity, activity.getString(R.string.budget_save_error));
                        });
                    }
                }
            });
        } else {
            // Could not extract amount, ask AI to help
            activity.runOnUiThread(() -> {
                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                        context.getString(R.string.budget_amount_not_recognized),
                        false, context.getString(R.string.now_label)));
                chatAdapter.notifyItemChanged(analyzingIndex);
            });
        }
    }

    /**
     * Handle delete budget request
     */
    public void handleDeleteBudget(String text, Context context, Activity activity,
                                        List<AiChatBottomSheet.ChatMessage> messages,
                                        AiChatBottomSheet.ChatAdapter chatAdapter,
                                        RecyclerView messagesRecycler,
                                        Runnable refreshHomeFragmentCallback) {
        // Add analyzing message
        int analyzingIndex = messages.size();
        messages.add(new AiChatBottomSheet.ChatMessage(context.getString(R.string.processing_delete_request), false, context.getString(R.string.now_label)));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        // Extract month and year from text
        int[] monthYear = DateParser.extractMonthYear(text);
        int targetMonth = monthYear[0];
        int targetYear = monthYear[1];

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Create calendar for target month
                Calendar targetCal = Calendar.getInstance();
                targetCal.set(Calendar.YEAR, targetYear);
                targetCal.set(Calendar.MONTH, targetMonth - 1);
                targetCal.set(Calendar.DAY_OF_MONTH, 1);
                targetCal.set(Calendar.HOUR_OF_DAY, 0);
                targetCal.set(Calendar.MINUTE, 0);
                targetCal.set(Calendar.SECOND, 0);
                targetCal.set(Calendar.MILLISECOND, 0);
                Date startOfMonth = targetCal.getTime();

                targetCal.set(Calendar.DAY_OF_MONTH, targetCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                targetCal.set(Calendar.HOUR_OF_DAY, 23);
                targetCal.set(Calendar.MINUTE, 59);
                targetCal.set(Calendar.SECOND, 59);
                Date endOfMonth = targetCal.getTime();

                // Check if budget exists
                int userId = userSession.getCurrentUserId();
                List<BudgetEntity> existingBudgets = budgetRepository
                        .getBudgetsByDateRange(userId, startOfMonth, endOfMonth);

                if (existingBudgets != null && !existingBudgets.isEmpty()) {
                    // Get the budget amount before deleting
                    long budgetAmount = existingBudgets.get(0).monthlyLimit;

                    // Delete budget
                    budgetRepository
                            .deleteBudgetsByDateRange(userId, startOfMonth, endOfMonth);

                    // Log budget history
                    BudgetHistoryLogger.logMonthlyBudgetDeleted(
                            context, budgetAmount, startOfMonth);

                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
                    String monthYearStr = monthYearFormat.format(startOfMonth);

                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            String responseMessage = String.format(context.getString(R.string.budget_deleted_success), monthYearStr);

                            messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(responseMessage, false, context.getString(R.string.now_label)));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);

                            ToastHelper.showToastOnTop(activity, String.format(activity.getString(R.string.budget_deleted_toast), monthYearStr));
                            refreshHomeFragmentCallback.run();
                        });
                    }
                } else {
                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
                    String monthYearStr = monthYearFormat.format(startOfMonth);

                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            String responseMessage = String.format(context.getString(R.string.budget_not_found_for_delete), monthYearStr);

                            messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(responseMessage, false, context.getString(R.string.now_label)));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                        });
                    }
                }

            } catch (Exception e) {
                android.util.Log.e("BudgetService", "Error deleting budget", e);

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                                context.getString(R.string.budget_delete_error_message),
                                false, context.getString(R.string.now_label)));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                        ToastHelper.showErrorToast(activity, activity.getString(R.string.budget_delete_error));
                    });
                }
            }
        });
    }

    /**
     * Handle budget analysis/view request (when user wants to view or analyze budget data)
     */
    public void handleBudgetAnalysis(String text, Context context, Activity activity,
                                          List<AiChatBottomSheet.ChatMessage> messages,
                                          AiChatBottomSheet.ChatAdapter chatAdapter,
                                          RecyclerView messagesRecycler,
                                          android.speech.tts.TextToSpeech textToSpeech,
                                          Runnable updateNetworkStatusCallback) {
        String lowerText = text.toLowerCase();

        // User wants to view or analyze budget - get budget data and send to AI
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String budgetContext = aiContextUseCase.getBudgetContext(context);

                // Detect if user wants detailed analysis/consultation or just viewing
                boolean needsDetailedAnalysis = lowerText.contains("phân tích") ||
                                               lowerText.contains("tư vấn") ||
                                               lowerText.contains("đánh giá") ||
                                               lowerText.contains("so sánh") ||
                                               lowerText.contains("xu hướng") ||
                                               lowerText.contains("dự báo") ||
                                               lowerText.contains("nhận xét") ||
                                               lowerText.contains("góp ý");

                // Add context prefix to help AI understand user's intent
                String queryWithContext = text;
                if (needsDetailedAnalysis) {
                    queryWithContext = "[YÊU CẦU PHÂN TÍCH CHI TIẾT] " + text;
                } else {
                    queryWithContext = "[CHỈ XEM THÔNG TIN] " + text;
                }

                String finalQuery = queryWithContext;
                activity.runOnUiThread(() -> {
                    aiContextUseCase.sendPromptToAIWithBudgetContext(context, finalQuery, budgetContext, messages, chatAdapter, messagesRecycler, textToSpeech, updateNetworkStatusCallback);
                });
            } catch (Exception e) {
                android.util.Log.e("BudgetService", "Error getting budget context", e);
                activity.runOnUiThread(() -> {
                    promptUseCase.sendPromptToAI(text, activity, messages, chatAdapter, messagesRecycler, textToSpeech, updateNetworkStatusCallback, () -> {});
                });
            }
        });
    }

    /**
     * Handle all budget queries (view, analyze, add, edit, delete)
     */
    public void handleBudgetQuery(String text, Context context, Activity activity,
                                       List<AiChatBottomSheet.ChatMessage> messages,
                                       AiChatBottomSheet.ChatAdapter chatAdapter,
                                       RecyclerView messagesRecycler,
                                       android.speech.tts.TextToSpeech textToSpeech,
                                       Runnable updateNetworkStatusCallback,
                                       Runnable refreshHomeFragmentCallback) {
        String lowerText = text.toLowerCase();

        // Check if user wants to delete budget (Vietnamese + English)
        if (lowerText.contains("xóa") || lowerText.contains("xoá") ||
            lowerText.contains("delete") || lowerText.contains("remove")) {
            handleDeleteBudget(text, context, activity, messages, chatAdapter, messagesRecycler, refreshHomeFragmentCallback);
            return;
        }

        // Check if user wants to add/edit/increase/decrease budget (Vietnamese + English)
        // Include: set, add, edit, increase, decrease keywords
        if (lowerText.contains("thêm") || lowerText.contains("đặt") ||
            lowerText.contains("sửa") || lowerText.contains("thay đổi") ||
            lowerText.contains("thiết lập") ||
            lowerText.contains("tăng") || lowerText.contains("nâng") ||
            lowerText.contains("giảm") || lowerText.contains("hạ") ||
            lowerText.contains("cộng") || lowerText.contains("trừ") ||
            lowerText.contains("bớt") || lowerText.contains("cắt") ||
            lowerText.contains("set") || lowerText.contains("add") ||
            lowerText.contains("edit") || lowerText.contains("change") ||
            lowerText.contains("establish") || lowerText.contains("establish") ||
            lowerText.contains("increase") || lowerText.contains("raise") ||
            lowerText.contains("decrease") || lowerText.contains("reduce") ||
            lowerText.contains("lower") || lowerText.contains("plus") ||
            lowerText.contains("minus") || lowerText.contains("cut") ||
            lowerText.contains("put") || lowerText.contains("create") ||
            lowerText.contains("make") || lowerText.contains("assign")) {
            handleBudgetRequest(text, context, activity, messages, chatAdapter, messagesRecycler, refreshHomeFragmentCallback);
            return;
        }

        // User wants to view or analyze budget - delegate to BudgetService
        handleBudgetAnalysis(text, context, activity, messages, chatAdapter, messagesRecycler, textToSpeech, updateNetworkStatusCallback);
    }
}