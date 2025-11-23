package com.example.spending_management_app.domain.usecase.budget;

import android.app.Activity;
import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.data.local.entity.BudgetEntity;
import com.example.spending_management_app.domain.repository.BudgetRepository;
import com.example.spending_management_app.domain.usecase.ai.AiContextUseCase;
import com.example.spending_management_app.domain.usecase.ai.PromptUseCase;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet;
import com.example.spending_management_app.utils.BudgetAmountParser;
import com.example.spending_management_app.utils.DateParser;
import com.example.spending_management_app.utils.ToastHelper;
import com.example.spending_management_app.utils.CurrencyFormatter;

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

    public BudgetUseCase(BudgetRepository budgetRepository, PromptUseCase promptUseCase, AiContextUseCase aiContextUseCase) {
        this.budgetRepository = budgetRepository;
        this.promptUseCase = promptUseCase;
        this.aiContextUseCase = aiContextUseCase;
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
        messages.add(new AiChatBottomSheet.ChatMessage("Đang xử lý yêu cầu...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        // Check if this is an increase/decrease request or absolute set request
        String textLower = text.toLowerCase().trim();

        android.util.Log.d("BudgetService", "=== BUDGET REQUEST DEBUG ===");
        android.util.Log.d("BudgetService", "Original text: [" + text + "]");
        android.util.Log.d("BudgetService", "Lowercase text: [" + textLower + "]");

        // Check for ABSOLUTE set commands with "lên" or "xuống"
        // "Tăng lên 10 triệu", "Nâng lên 10 triệu", "Hạ xuống 10 triệu", "Giảm xuống 10 triệu"
        boolean hasLenKeyword = textLower.contains("lên");
        boolean hasXuongKeyword = textLower.contains("xuống");
        boolean isAbsoluteSet = ((textLower.contains("tăng") || textLower.contains("nâng")) && hasLenKeyword) ||
                                ((textLower.contains("giảm") || textLower.contains("hạ")) && hasXuongKeyword);

        android.util.Log.d("BudgetService", "Has 'lên': " + hasLenKeyword + ", Has 'xuống': " + hasXuongKeyword);
        android.util.Log.d("BudgetService", "isAbsoluteSet: " + isAbsoluteSet);

        // Check for RELATIVE increase (add more) - only if NOT absolute set
        // "Nâng ngân sách 10 triệu", "Tăng ngân sách 10 triệu", "Tăng thêm 10 triệu"
        boolean hasIncreaseKeyword = textLower.contains("nâng") ||
                                     textLower.contains("tăng") ||
                                     textLower.contains("cộng") ||
                                     textLower.contains("thêm");
        boolean isIncrease = !isAbsoluteSet && hasIncreaseKeyword;

        android.util.Log.d("BudgetService", "Has increase keyword: " + hasIncreaseKeyword + ", isIncrease: " + isIncrease);

        // Check for RELATIVE decrease (subtract) - only if NOT absolute set
        // "Giảm ngân sách 2 triệu", "Hạ ngân sách 1 triệu", "Trừ 2 triệu"
        boolean hasDecreaseKeyword = textLower.contains("giảm") ||
                                     textLower.contains("hạ") ||
                                     textLower.contains("trừ") ||
                                     textLower.contains("bớt") ||
                                     textLower.contains("cắt");
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
                        "⚠️ Không thể thêm hoặc sửa ngân sách cho tháng trong quá khứ!\n\n" +
                        "Bạn chỉ có thể quản lý ngân sách từ tháng " + currentMonth + "/" + currentYear + " trở đi.",
                        false, "Bây giờ"));
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

                    List<BudgetEntity> existingBudgets = budgetRepository
                            .getBudgetsByDateRangeOrdered(startOfMonth, endOfMonth);

                    android.util.Log.d("BudgetService", "Found " + (existingBudgets != null ? existingBudgets.size() : 0) + " existing budgets");

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
                                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
                                    String monthYearStr = monthYearFormat.format(budgetDate);
                                    messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                                            "⚠️ Chưa có ngân sách cho tháng " + monthYearStr + " để " +
                                            (isIncrease ? "nâng" : "giảm") + "!\n\n" +
                                            "Vui lòng đặt ngân sách trước. Ví dụ:\n" +
                                            "   • \"Đặt ngân sách tháng " + monthYearStr + " là 15 triệu\"",
                                            false, "Bây giờ"));
                                    chatAdapter.notifyItemChanged(analyzingIndex);
                                });
                            }
                            return; // Exit without creating new budget
                        }

                        calculatedFinalAmount = amount;
                        determinedActionType = "set";

                        BudgetEntity budget = new BudgetEntity("Ngân sách tháng", calculatedFinalAmount, 0L, budgetDate);
                        android.util.Log.d("BudgetService", "Inserting new budget: " + budget.date);
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
                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
                    String monthYearStr = monthYearFormat.format(budgetDate);

                    // Update UI
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            String responseMessage;
                            String toastMessage;

                            if (isUpdate) {
                                if (actionType.equals("increase")) {
                                    responseMessage = "✅ Đã nâng ngân sách tháng " + monthYearStr + " thêm " + formattedChangeAmount + "!\n\n" +
                                            "💰 Ngân sách mới: " + formattedFinalAmount + "\n\n" +
                                            "Chúc bạn quản lý tài chính tốt! 💪";
                                    toastMessage = "✅ Đã nâng ngân sách tháng " + monthYearStr + ": +" + formattedChangeAmount;
                                } else if (actionType.equals("decrease")) {
                                    responseMessage = "✅ Đã giảm ngân sách tháng " + monthYearStr + " xuống " + formattedChangeAmount + "!\n\n" +
                                            "💰 Ngân sách mới: " + formattedFinalAmount + "\n\n" +
                                            "Chúc bạn chi tiêu hợp lý! 💰";
                                    toastMessage = "✅ Đã giảm ngân sách tháng " + monthYearStr + ": -" + formattedChangeAmount;
                                } else {
                                    responseMessage = "✅ Đã cập nhật ngân sách tháng " + monthYearStr + " thành " + formattedFinalAmount + "!\n\n" +
                                            "Chúc bạn quản lý tài chính tốt! 💪";
                                    toastMessage = "✅ Đã cập nhật ngân sách tháng " + monthYearStr + ": " + formattedFinalAmount;
                                }
                            } else {
                                responseMessage = "✅ Đã thiết lập ngân sách tháng " + monthYearStr + " là " + formattedFinalAmount + "!\n\n" +
                                        "Chúc bạn chi tiêu hợp lý! 💰";
                                toastMessage = "✅ Đã thiết lập ngân sách tháng " + monthYearStr + ": " + formattedFinalAmount;
                            }

                            messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(responseMessage, false, "Bây giờ"));
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
                                    "❌ Có lỗi xảy ra khi lưu ngân sách. Vui lòng thử lại!",
                                    false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            ToastHelper.showErrorToast(activity, "Lỗi lưu ngân sách");
                        });
                    }
                }
            });
        } else {
            // Could not extract amount, ask AI to help
            activity.runOnUiThread(() -> {
                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                        "🤔 Tôi không thể xác định số tiền ngân sách từ yêu cầu của bạn.\n\n" +
                        "Vui lòng nhập rõ số tiền và tháng (nếu cần), ví dụ:\n\n" +
                        "📝 Đặt ngân sách:\n" +
                        "   • \"Đặt ngân sách tháng này 15 triệu\"\n" +
                        "   • \"Đặt ngân sách tháng 12 là 20 triệu\"\n\n" +
                        "➕ Tăng thêm (cộng vào ngân sách hiện tại):\n" +
                        "   • \"Nâng ngân sách 2 triệu\"\n" +
                        "   • \"Tăng thêm 1.5 triệu\"\n\n" +
                        "➖ Giảm bớt (trừ khỏi ngân sách hiện tại):\n" +
                        "   • \"Giảm ngân sách 500k\"\n" +
                        "   • \"Trừ 1 triệu\"\n\n" +
                        "🎯 Đặt lại thành số cụ thể:\n" +
                        "   • \"Tăng ngân sách lên 10 triệu\"\n" +
                        "   • \"Giảm ngân sách xuống 8 triệu\"",
                        false, "Bây giờ"));
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
        messages.add(new AiChatBottomSheet.ChatMessage("Đang xử lý yêu cầu xóa...", false, "Bây giờ"));
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
                List<BudgetEntity> existingBudgets = budgetRepository
                        .getBudgetsByDateRange(startOfMonth, endOfMonth);

                if (existingBudgets != null && !existingBudgets.isEmpty()) {
                    // Get the budget amount before deleting
                    long budgetAmount = existingBudgets.get(0).monthlyLimit;

                    // Delete budget
                    budgetRepository
                            .deleteBudgetsByDateRange(startOfMonth, endOfMonth);

                    // Log budget history
                    BudgetHistoryLogger.logMonthlyBudgetDeleted(
                            context, budgetAmount, startOfMonth);

                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
                    String monthYearStr = monthYearFormat.format(startOfMonth);

                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            String responseMessage = "✅ Đã xóa ngân sách tháng " + monthYearStr + "!\n\n" +
                                    "Bạn có thể thiết lập lại bất cứ lúc nào. 💰";

                            messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(responseMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            messagesRecycler.smoothScrollToPosition(messages.size() - 1);

                            ToastHelper.showToastOnTop(activity, "✅ Đã xóa ngân sách tháng " + monthYearStr);
                            refreshHomeFragmentCallback.run();
                        });
                    }
                } else {
                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
                    String monthYearStr = monthYearFormat.format(startOfMonth);

                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            String responseMessage = "⚠️ Không tìm thấy ngân sách tháng " + monthYearStr + " để xóa!";

                            messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(responseMessage, false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                        });
                    }
                }

            } catch (Exception e) {
                android.util.Log.e("BudgetService", "Error deleting budget", e);

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                                "❌ Có lỗi xảy ra khi xóa ngân sách. Vui lòng thử lại!",
                                false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                        ToastHelper.showErrorToast(activity, "Lỗi xóa ngân sách");
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
                    promptUseCase.sendPromptToAI(text, activity, messages, chatAdapter, messagesRecycler, textToSpeech, updateNetworkStatusCallback);
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

        // Check if user wants to delete budget
        if (lowerText.contains("xóa") || lowerText.contains("xoá")) {
            handleDeleteBudget(text, context, activity, messages, chatAdapter, messagesRecycler, refreshHomeFragmentCallback);
            return;
        }

        // Check if user wants to add/edit/increase/decrease budget
        // Include: set, add, edit, increase, decrease keywords
        if (lowerText.contains("thêm") || lowerText.contains("đặt") ||
            lowerText.contains("sửa") || lowerText.contains("thay đổi") ||
            lowerText.contains("thiết lập") ||
            lowerText.contains("tăng") || lowerText.contains("nâng") ||
            lowerText.contains("giảm") || lowerText.contains("hạ") ||
            lowerText.contains("cộng") || lowerText.contains("trừ") ||
            lowerText.contains("bớt") || lowerText.contains("cắt")) {
            handleBudgetRequest(text, context, activity, messages, chatAdapter, messagesRecycler, refreshHomeFragmentCallback);
            return;
        }

        // User wants to view or analyze budget - delegate to BudgetService
        handleBudgetAnalysis(text, context, activity, messages, chatAdapter, messagesRecycler, textToSpeech, updateNetworkStatusCallback);
    }
}