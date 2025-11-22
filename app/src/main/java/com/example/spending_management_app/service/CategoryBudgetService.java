package com.example.spending_management_app.service;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.BudgetEntity;
import com.example.spending_management_app.database.CategoryBudgetEntity;
import com.example.spending_management_app.ui.AiChatBottomSheet;
import com.example.spending_management_app.utils.BudgetHistoryLogger;
import com.example.spending_management_app.utils.CategoryIconHelper;
import com.example.spending_management_app.utils.ToastHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Service class for handling category budget management operations
 */
public class CategoryBudgetService {

    /**
     * Handle category budget request (add, edit, delete category budgets)
     */
    public static void handleCategoryBudgetRequest(String text, Context context, Activity activity,
                                                 List<AiChatBottomSheet.ChatMessage> messages,
                                                 AiChatBottomSheet.ChatAdapter chatAdapter,
                                                 RecyclerView messagesRecycler,
                                                 Runnable refreshHomeFragmentCallback,
                                                 Runnable refreshCategoryBudgetWelcomeMessageCallback) {
        android.util.Log.d("CategoryBudgetService", "handleCategoryBudgetRequest: " + text);

        // Add analyzing message
        int analyzingIndex = messages.size();
        messages.add(new AiChatBottomSheet.ChatMessage("Đang xử lý yêu cầu...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        String lowerText = text.toLowerCase();

        // Parse multiple operations from text
        List<CategoryBudgetParserService.CategoryBudgetOperation> operations = CategoryBudgetParserService.parseMultipleCategoryOperations(text);

        if (operations.isEmpty()) {
            // Unknown command
            activity.runOnUiThread(() -> {
                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                        "⚠️ Không hiểu yêu cầu của bạn.\n\n" +
                        "💡 Hướng dẫn:\n" +
                        "• Đặt: 'Đặt ngân sách ăn uống 2 triệu'\n" +
                        "• Sửa: 'Sửa ngân sách di chuyển 1 triệu'\n" +
                        "• Xóa: 'Xóa ngân sách cafe'\n" +
                        "• Nhiều: 'Thêm 500k ăn uống và 300k di chuyển'",
                        false, "Bây giờ"));
                chatAdapter.notifyItemChanged(analyzingIndex);
            });
            return;
        }

        // Process all operations
        processCategoryBudgetOperations(operations, analyzingIndex, context, activity, messages, chatAdapter, messagesRecycler, refreshHomeFragmentCallback, refreshCategoryBudgetWelcomeMessageCallback);
    }

    /**
     * Process category budget operations
     */
    private static void processCategoryBudgetOperations(List<CategoryBudgetParserService.CategoryBudgetOperation> operations, int analyzingIndex,
                                                      Context context, Activity activity,
                                                      List<AiChatBottomSheet.ChatMessage> messages,
                                                      AiChatBottomSheet.ChatAdapter chatAdapter,
                                                      RecyclerView messagesRecycler,
                                                      Runnable refreshHomeFragmentCallback,
                                                      Runnable refreshCategoryBudgetWelcomeMessageCallback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get current month range
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

                // Get monthly budget to check limit
                List<BudgetEntity> monthlyBudgets =
                        AppDatabase.getInstance(context).budgetDao()
                                .getBudgetsByDateRange(startOfMonth, endOfMonth);
                long monthlyBudgetLimit = (monthlyBudgets != null && !monthlyBudgets.isEmpty())
                        ? monthlyBudgets.get(0).getMonthlyLimit() : 0;

                StringBuilder resultMessage = new StringBuilder();
                final int[] counts = new int[]{0, 0}; // [0] = successCount, [1] = failCount

                // Check if this is a "delete all" operation
                if (!operations.isEmpty() && operations.get(0).type.equals("delete_all")) {
                    try {
                        // Get all category budgets for current month
                        List<CategoryBudgetEntity> allBudgets =
                                AppDatabase.getInstance(context).categoryBudgetDao()
                                        .getAllCategoryBudgetsForMonth(startOfMonth, endOfMonth);

                        if (allBudgets != null && !allBudgets.isEmpty()) {
                            // Delete all category budgets
                            for (CategoryBudgetEntity budget : allBudgets) {
                                AppDatabase.getInstance(context).categoryBudgetDao().delete(budget);
                                counts[0]++;
                            }

                            // Log budget history for delete all
                            BudgetHistoryLogger.logAllCategoryBudgetsDeleted(context);

                            resultMessage.append("✅ Đã xóa tất cả ngân sách danh mục (")
                                    .append(counts[0]).append(" danh mục)\n\n");
                            resultMessage.append("💡 Tất cả danh mục đã được đặt lại về trạng thái 'Chưa thiết lập'");
                        } else {
                            resultMessage.append("⚠️ Không có ngân sách danh mục nào để xóa!");
                            counts[1]++;
                        }

                        String finalMessage = resultMessage.toString();

                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(finalMessage, false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);

                                if (counts[0] > 0) {
                                    ToastHelper.showToastOnTop(activity, "✅ Đã xóa tất cả ngân sách danh mục");
                                    refreshHomeFragmentCallback.run();
                                    refreshCategoryBudgetWelcomeMessageCallback.run();
                                } else {
                                    ToastHelper.showErrorToast(activity, "⚠️ Không có ngân sách nào để xóa");
                                }
                            });
                        }

                    } catch (Exception e) {
                        android.util.Log.e("CategoryBudgetService", "Error deleting all category budgets", e);

                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                                        "❌ Có lỗi xảy ra khi xóa tất cả ngân sách danh mục!",
                                        false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                ToastHelper.showErrorToast(activity, "Lỗi xóa ngân sách");
                            });
                        }
                    }
                    return; // Exit early, don't process other operations
                }

                for (CategoryBudgetParserService.CategoryBudgetOperation op : operations) {
                    try {
                        if (op.type.equals("delete")) {
                            // Delete operation
                            CategoryBudgetEntity existing =
                                    AppDatabase.getInstance(context)
                                            .categoryBudgetDao()
                                            .getCategoryBudgetForMonth(op.category, startOfMonth, endOfMonth);

                            if (existing != null) {
                                long deletedAmount = existing.budgetAmount;
                                AppDatabase.getInstance(context).categoryBudgetDao().delete(existing);

                                // Log budget history
                                BudgetHistoryLogger.logCategoryBudgetDeleted(
                                        context, op.category, deletedAmount);

                                String icon = CategoryIconHelper.getIconEmoji(op.category);
                                resultMessage.append("✅ Xóa ").append(icon).append(" ").append(op.category).append("\n");
                                counts[0]++;
                            } else {
                                resultMessage.append("⚠️ ").append(op.category).append(": Không tìm thấy\n");
                                counts[1]++;
                            }
                        } else {
                            // Add or Edit operation
                            CategoryBudgetEntity existing =
                                    AppDatabase.getInstance(context)
                                            .categoryBudgetDao()
                                            .getCategoryBudgetForMonth(op.category, startOfMonth, endOfMonth);

                            boolean isUpdate = (existing != null);

                            // Check if adding/updating will exceed monthly budget
                            if (monthlyBudgetLimit > 0) {
                                List<CategoryBudgetEntity> allCategoryBudgets =
                                        AppDatabase.getInstance(context).categoryBudgetDao()
                                                .getAllCategoryBudgetsForMonth(startOfMonth, endOfMonth);

                                long currentTotal = 0;
                                for (CategoryBudgetEntity cb : allCategoryBudgets) {
                                    if (!cb.getCategory().equals(op.category)) {
                                        currentTotal += cb.getBudgetAmount();
                                    }
                                }

                                long newTotal = currentTotal + op.amount;

                                if (newTotal > monthlyBudgetLimit) {
                                    String icon = CategoryIconHelper.getIconEmoji(op.category);
                                    long available = monthlyBudgetLimit - currentTotal;
                                    resultMessage.append(String.format("⚠️ %s %s: Vượt ngân sách tháng %,d VND (Ngân sách còn lại: %,d VND)\n",
                                            icon, op.category, monthlyBudgetLimit, available));
                                    counts[1]++;
                                    continue;
                                }
                            }

                            if (isUpdate) {
                                long oldAmount = existing.budgetAmount;
                                existing.budgetAmount = op.amount;
                                AppDatabase.getInstance(context).categoryBudgetDao().update(existing);

                                // Log budget history
                                BudgetHistoryLogger.logCategoryBudgetUpdated(
                                        context, op.category, oldAmount, op.amount);
                            } else {
                                CategoryBudgetEntity newBudget =
                                        new CategoryBudgetEntity(
                                                op.category, op.amount, startOfMonth);
                                AppDatabase.getInstance(context).categoryBudgetDao().insert(newBudget);

                                // Log budget history
                                BudgetHistoryLogger.logCategoryBudgetCreated(
                                        context, op.category, op.amount);
                            }

                            String icon = CategoryIconHelper.getIconEmoji(op.category);
                            String formattedAmount = String.format("%,d", op.amount);
                            String action = isUpdate ? "Sửa" : "Thêm";
                            resultMessage.append("✅ ").append(action).append(" ").append(icon).append(" ")
                                    .append(op.category).append(": ").append(formattedAmount).append(" VND\n");
                            counts[0]++;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("CategoryBudgetService", "Error processing operation for " + op.category, e);
                        resultMessage.append("❌ ").append(op.category).append(": Lỗi\n");
                        counts[1]++;
                    }
                }

                // Add summary
                resultMessage.append("\n📊 Kết quả: ")
                        .append(counts[0]).append(" thành công");
                if (counts[1] > 0) {
                    resultMessage.append(", ").append(counts[1]).append(" thất bại");
                }

                // If there are successful operations, show remaining budget info
                if (counts[0] > 0 && monthlyBudgetLimit > 0) {
                    // Recalculate total after all operations
                    List<CategoryBudgetEntity> updatedBudgets =
                            AppDatabase.getInstance(context).categoryBudgetDao()
                                    .getAllCategoryBudgetsForMonth(startOfMonth, endOfMonth);

                    long totalUsed = 0;
                    for (CategoryBudgetEntity cb : updatedBudgets) {
                        totalUsed += cb.getBudgetAmount();
                    }

                    long remaining = monthlyBudgetLimit - totalUsed;
                    resultMessage.append("\n\n💰 Ngân sách tháng: ").append(String.format("%,d", monthlyBudgetLimit)).append(" VND");
                    resultMessage.append("\n📈 Đã phân bổ: ").append(String.format("%,d", totalUsed)).append(" VND");

                    if (remaining >= 0) {
                        resultMessage.append("\n✅ Còn lại: ").append(String.format("%,d", remaining)).append(" VND");
                    } else {
                        resultMessage.append("\n⚠️ Vượt quá: ").append(String.format("%,d", Math.abs(remaining))).append(" VND");
                    }
                }

                String finalMessage = resultMessage.toString();

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(finalMessage, false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);

                        // Show toast based on result
                        if (counts[1] > 0) {
                            // Has failures - show error toast in red
                            if (counts[0] > 0) {
                                // Mixed results
                                ToastHelper.showErrorToast(activity, "⚠️ " + counts[0] + " thành công, " + counts[1] + " thất bại");
                            } else {
                                // All failed
                                ToastHelper.showErrorToast(activity, "❌ Thất bại: " + counts[1] + " danh mục");
                            }
                        } else {
                            // All success - show success toast in green
                            ToastHelper.showToastOnTop(activity, "✅ Cập nhật " + counts[0] + " danh mục");
                        }

                        refreshHomeFragmentCallback.run();

                        // Refresh welcome message with updated data
                        refreshCategoryBudgetWelcomeMessageCallback.run();
                    });
                }

            } catch (Exception e) {
                android.util.Log.e("CategoryBudgetService", "Error processing category budget operations", e);

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                                "❌ Có lỗi xảy ra khi xử lý yêu cầu!",
                                false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                    });
                }
            }
        });
    }
}