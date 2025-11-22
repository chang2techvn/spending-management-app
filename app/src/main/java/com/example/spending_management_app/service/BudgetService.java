package com.example.spending_management_app.service;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.BudgetEntity;
import com.example.spending_management_app.ui.AiChatBottomSheet;
import com.example.spending_management_app.utils.BudgetAmountParser;
import com.example.spending_management_app.utils.DateParser;
import com.example.spending_management_app.utils.ToastHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Service class for handling budget management operations
 */
public class BudgetService {

    /**
     * Handle budget request (add, edit, increase, decrease budget)
     */
    public static void handleBudgetRequest(String text, Context context, Activity activity,
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

                    List<BudgetEntity> existingBudgets = AppDatabase.getInstance(context)
                            .budgetDao()
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
                        AppDatabase.getInstance(context).budgetDao().update(existing);

                        // Log budget history
                        com.example.spending_management_app.utils.BudgetHistoryLogger.logMonthlyBudgetUpdated(
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
                        AppDatabase.getInstance(context).budgetDao().insert(budget);

                        // Log budget history
                        com.example.spending_management_app.utils.BudgetHistoryLogger.logMonthlyBudgetCreated(
                                context, calculatedFinalAmount, budgetDate);
                    }

                    // Make final variables for lambda
                    final long finalAmount = calculatedFinalAmount;
                    final String actionType = determinedActionType;

                    String formattedFinalAmount = String.format("%,d", finalAmount);
                    String formattedChangeAmount = String.format("%,d", amount);
                    SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
                    String monthYearStr = monthYearFormat.format(budgetDate);

                    // Update UI
                    if (activity != null) {
                        activity.runOnUiThread(() -> {
                            String responseMessage;
                            String toastMessage;

                            if (isUpdate) {
                                if (actionType.equals("increase")) {
                                    responseMessage = "✅ Đã nâng ngân sách tháng " + monthYearStr + " thêm " + formattedChangeAmount + " VND!\n\n" +
                                            "💰 Ngân sách mới: " + formattedFinalAmount + " VND\n\n" +
                                            "Chúc bạn quản lý tài chính tốt! 💪";
                                    toastMessage = "✅ Đã nâng ngân sách tháng " + monthYearStr + ": +" + formattedChangeAmount + " VND";
                                } else if (actionType.equals("decrease")) {
                                    responseMessage = "✅ Đã giảm ngân sách tháng " + monthYearStr + " xuống " + formattedChangeAmount + " VND!\n\n" +
                                            "💰 Ngân sách mới: " + formattedFinalAmount + " VND\n\n" +
                                            "Chúc bạn chi tiêu hợp lý! 💰";
                                    toastMessage = "✅ Đã giảm ngân sách tháng " + monthYearStr + ": -" + formattedChangeAmount + " VND";
                                } else {
                                    responseMessage = "✅ Đã cập nhật ngân sách tháng " + monthYearStr + " thành " + formattedFinalAmount + " VND!\n\n" +
                                            "Chúc bạn quản lý tài chính tốt! 💪";
                                    toastMessage = "✅ Đã cập nhật ngân sách tháng " + monthYearStr + ": " + formattedFinalAmount + " VND";
                                }
                            } else {
                                responseMessage = "✅ Đã thiết lập ngân sách tháng " + monthYearStr + " là " + formattedFinalAmount + " VND!\n\n" +
                                        "Chúc bạn chi tiêu hợp lý! 💰";
                                toastMessage = "✅ Đã thiết lập ngân sách tháng " + monthYearStr + ": " + formattedFinalAmount + " VND";
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
}