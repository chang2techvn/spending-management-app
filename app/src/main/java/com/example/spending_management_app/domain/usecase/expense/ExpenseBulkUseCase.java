package com.example.spending_management_app.domain.usecase.expense;

import android.content.Context;

import com.example.spending_management_app.data.local.entity.TransactionEntity;
import com.example.spending_management_app.domain.repository.ExpenseRepository;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet;
import com.example.spending_management_app.utils.BudgetAmountParser;
import com.example.spending_management_app.utils.CategoryIconHelper;
import com.example.spending_management_app.utils.DateParser;
import com.example.spending_management_app.utils.ExtractorHelper;
import com.example.spending_management_app.utils.ToastHelper;
import com.example.spending_management_app.utils.CurrencyFormatter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpenseBulkUseCase {

    private final ExpenseRepository expenseRepository;

    public ExpenseBulkUseCase(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    // ==================== EXPENSE BULK MANAGEMENT ====================

    public void handleExpenseBulkRequest(String text, Context context,
            android.app.Activity activity, List<AiChatBottomSheet.ChatMessage> messages,
            AiChatBottomSheet.ChatAdapter chatAdapter, androidx.recyclerview.widget.RecyclerView messagesRecycler,
            Runnable refreshHomeFragment, Runnable refreshExpenseWelcomeMessage) {

        android.util.Log.d("ExpenseBulkService", "handleExpenseBulkRequest: " + text);

        // Add analyzing message
        int analyzingIndex = messages.size();
        messages.add(new AiChatBottomSheet.ChatMessage("Đang xử lý yêu cầu...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        String lowerText = text.toLowerCase();

        // Parse multiple expense operations from text
        List<ExpenseOperation> operations = parseMultipleExpenseOperations(text);

        if (operations.isEmpty()) {
            // Unknown command
            activity.runOnUiThread(() -> {
                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                        "⚠️ Không hiểu yêu cầu của bạn.\n\n" +
                        "💡 Hướng dẫn:\n" +
                        "• Thêm: 'Hôm qua ăn sáng 25k và cafe 30k'\n" +
                        "• Sửa: 'Sửa chi tiêu [ID] thành 50k'\n" +
                        "• Xóa: 'Xóa chi tiêu [ID]'",
                        false, "Bây giờ"));
                chatAdapter.notifyItemChanged(analyzingIndex);
            });
            return;
        }

        // Process all operations
        processExpenseOperations(operations, analyzingIndex, context, activity, messages, chatAdapter, messagesRecycler, refreshHomeFragment, refreshExpenseWelcomeMessage);
    }

    // Helper class for expense operations
    public static class ExpenseOperation {
        String type; // "add", "edit", "delete"
        String description;
        String category;
        long amount;
        Date date;
        int transactionId; // For edit/delete operations

        ExpenseOperation(String type, String description, String category, long amount, Date date) {
            this.type = type;
            this.description = description;
            this.category = category;
            this.amount = amount;
            this.date = date;
            this.transactionId = -1;
        }

        ExpenseOperation(String type, int transactionId) {
            this.type = type;
            this.transactionId = transactionId;
            this.description = "";
            this.category = "";
            this.amount = 0;
            this.date = new Date();
        }
    }

    private static List<ExpenseOperation> parseMultipleExpenseOperations(String text) {
        List<ExpenseOperation> operations = new ArrayList<>();
        String lowerText = text.toLowerCase();

        // Determine operation type
        String operationType = "add"; // default
        if (lowerText.contains("xóa") || lowerText.contains("xoá")) {
            operationType = "delete";
        } else if (lowerText.contains("sửa") || lowerText.contains("thay đổi") || lowerText.contains("cập nhật")) {
            operationType = "edit";
        }

        // For edit/delete, try to extract transaction ID
        if (operationType.equals("delete") || operationType.equals("edit")) {
            // Try to find ID pattern like "#123", "ID 123", "id:123"
            Pattern idPattern = Pattern.compile("(?:#|id[:\\s]+)(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = idPattern.matcher(lowerText);

            while (matcher.find()) {
                int transactionId = Integer.parseInt(matcher.group(1));
                operations.add(new ExpenseOperation(operationType, transactionId));
            }

            // If no ID found but user said delete/edit, inform them
            if (operations.isEmpty()) {
                // Return empty - will show error message
                return operations;
            }
        }

        // For add operations, parse expenses from text
        if (operationType.equals("add")) {
            operations = parseExpensesFromText(text);
        }

        return operations;
    }

    private static List<ExpenseOperation> parseExpensesFromText(String text) {
        List<ExpenseOperation> operations = new ArrayList<>();

        android.util.Log.d("ExpenseBulkService", "=== parseExpensesFromText START ===");
        android.util.Log.d("ExpenseBulkService", "Input text: [" + text + "]");

        // List of all categories with their aliases
        java.util.Map<String, String> categoryAliases = new java.util.HashMap<>();

        // Full category names
        String[] allCategories = {
            "Ăn uống", "Di chuyển", "Tiện ích", "Y tế", "Nhà ở",
            "Mua sắm", "Giáo dục", "Sách & Học tập", "Thể thao", "Sức khỏe & Làm đẹp",
            "Giải trí", "Du lịch", "Ăn ngoài & Cafe", "Quà tặng & Từ thiện", "Hội họp & Tiệc tụng",
            "Điện thoại & Internet", "Đăng ký & Dịch vụ", "Phần mềm & Apps", "Ngân hàng & Phí",
            "Con cái", "Thú cưng", "Gia đình", "Khác"
        };

        // Add aliases
        categoryAliases.put("ăn sáng", "Ăn uống");
        categoryAliases.put("ăn trưa", "Ăn uống");
        categoryAliases.put("ăn tối", "Ăn uống");
        categoryAliases.put("cafe", "Ăn ngoài & Cafe");
        categoryAliases.put("cà phê", "Ăn ngoài & Cafe");
        categoryAliases.put("cơm", "Ăn uống");
        categoryAliases.put("xăng", "Di chuyển");
        categoryAliases.put("xe", "Di chuyển");
        categoryAliases.put("taxi", "Di chuyển");
        categoryAliases.put("grab", "Di chuyển");
        categoryAliases.put("bus", "Di chuyển");
        categoryAliases.put("điện", "Tiện ích");
        categoryAliases.put("nước", "Tiện ích");
        categoryAliases.put("internet", "Điện thoại & Internet");
        categoryAliases.put("điện thoại", "Điện thoại & Internet");
        categoryAliases.put("phim", "Giải trí");
        categoryAliases.put("game", "Giải trí");

        // First, split by newlines to handle multi-line input
        String[] lines = text.split("\\r?\\n");

        android.util.Log.d("ExpenseBulkService", "Number of lines: " + lines.length);
        for (int i = 0; i < lines.length; i++) {
            android.util.Log.d("ExpenseBulkService", "Line " + i + ": [" + lines[i] + "]");
        }

        // Process each line separately
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            android.util.Log.d("ExpenseBulkService", "Processing line: [" + line + "]");

            // Extract date from this line (each line can have its own date)
            Date expenseDate = DateParser.extractDateFromText(line);
            android.util.Log.d("ExpenseBulkService", "Extracted date: " + expenseDate);

            // Split each line by common separators (và, ,, ;)
            String[] segments = line.split("\\s+(và|,|;)\\s+");

            android.util.Log.d("ExpenseBulkService", "Number of segments in this line: " + segments.length);
            for (int i = 0; i < segments.length; i++) {
                android.util.Log.d("ExpenseBulkService", "  Segment " + i + ": [" + segments[i] + "]");
            }

            for (String segment : segments) {
                segment = segment.trim();
                if (segment.isEmpty()) continue;

                android.util.Log.d("ExpenseBulkService", "  Processing segment: [" + segment + "]");

                // Try to extract: description, amount, and category
                String description = "";
                String category = "Khác"; // default
                long amount = 0;

                // Extract amount
                amount = BudgetAmountParser.extractBudgetAmount(segment);
                android.util.Log.d("ExpenseBulkService", "    Extracted amount: " + amount);

                if (amount <= 0) {
                    android.util.Log.d("ExpenseBulkService", "    Skipping - no valid amount");
                    continue; // Skip if no valid amount
                }

                // Try to match category
                String matchedCategory = null;

                // First try full category names
                for (String cat : allCategories) {
                    if (segment.toLowerCase().contains(cat.toLowerCase())) {
                        matchedCategory = cat;
                        break;
                    }
                }

                // If no match, try aliases
                if (matchedCategory == null) {
                    for (java.util.Map.Entry<String, String> alias : categoryAliases.entrySet()) {
                        if (segment.toLowerCase().contains(alias.getKey())) {
                            matchedCategory = alias.getValue();
                            break;
                        }
                    }
                }

                if (matchedCategory != null) {
                    category = matchedCategory;
                }

                android.util.Log.d("ExpenseBulkService", "    Matched category: " + category);

                // Extract description (everything except amount and category keywords)
                description = ExtractorHelper.extractDescription(segment, category, amount);

                if (description.isEmpty()) {
                    description = category; // Use category as description if no description found
                }

                android.util.Log.d("ExpenseBulkService", "    Final description: " + description);
                android.util.Log.d("ExpenseBulkService", "    Creating expense: " + description + " - " + amount + " - " + category + " - " + expenseDate);

                operations.add(new ExpenseOperation("add", description, category, amount, expenseDate));
            }
        }

        android.util.Log.d("ExpenseBulkService", "Total operations created: " + operations.size());
        android.util.Log.d("ExpenseBulkService", "=== parseExpensesFromText END ===");

        return operations;
    }

    private void processExpenseOperations(List<ExpenseOperation> operations, int analyzingIndex,
            Context context, android.app.Activity activity, List<AiChatBottomSheet.ChatMessage> messages,
            AiChatBottomSheet.ChatAdapter chatAdapter, androidx.recyclerview.widget.RecyclerView messagesRecycler,
            Runnable refreshHomeFragment, Runnable refreshExpenseWelcomeMessage) {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                StringBuilder resultMessage = new StringBuilder();
                int[] counts = {0, 0}; // success, failure

                for (ExpenseOperation op : operations) {
                    try {
                        if (op.type.equals("delete")) {
                            // Delete transaction
                            TransactionEntity transaction = expenseRepository.getTransactionById(op.transactionId);

                            if (transaction != null) {
                                expenseRepository.delete(transaction);
                                resultMessage.append("✅ Xóa: ").append(transaction.description)
                                        .append(" (").append(CurrencyFormatter.formatCurrency(context, Math.abs(transaction.amount)))
                                        .append(")\n");
                                counts[0]++;
                            } else {
                                resultMessage.append("⚠️ Không tìm thấy chi tiêu #").append(op.transactionId).append("\n");
                                counts[1]++;
                            }

                        } else if (op.type.equals("edit")) {
                            // Edit transaction (not fully implemented in parse, just delete for now)
                            resultMessage.append("⚠️ Chức năng sửa chưa được hỗ trợ. Vui lòng xóa và thêm lại.\n");
                            counts[1]++;

                        } else if (op.type.equals("add")) {
                            // Add new transaction
                            TransactionEntity newTransaction = new TransactionEntity(
                                    op.description,
                                    op.category,
                                    -Math.abs(op.amount), // Expense is negative
                                    op.date,
                                    "expense"
                            );

                            expenseRepository.insert(newTransaction);

                            String icon = CategoryIconHelper.getIconEmoji(op.category);
                            resultMessage.append("✅ Thêm ").append(icon).append(" ")
                                    .append(op.description).append(": ")
                                    .append(CurrencyFormatter.formatCurrency(context, op.amount))
                                    .append(" (").append(op.category).append(")\n");
                            counts[0]++;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ExpenseBulkService", "Error processing expense operation", e);
                        resultMessage.append("❌ Lỗi xử lý: ").append(op.description).append("\n");
                        counts[1]++;
                    }
                }

                // Add summary
                resultMessage.append("\n📊 Kết quả: ")
                        .append(counts[0]).append(" thành công");
                if (counts[1] > 0) {
                    resultMessage.append(", ").append(counts[1]).append(" thất bại");
                }

                String finalMessage = resultMessage.toString();

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(finalMessage, false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);

                        // Show toast based on result
                        if (counts[1] > 0) {
                            if (counts[0] > 0) {
                                ToastHelper.showErrorToast(activity, "⚠️ " + counts[0] + " thành công, " + counts[1] + " thất bại");
                            } else {
                                ToastHelper.showErrorToast(activity, "❌ Thất bại: " + counts[1] + " giao dịch");
                            }
                        } else {
                            ToastHelper.showToastOnTop(activity, "✅ Thêm " + counts[0] + " chi tiêu");
                        }

                        refreshHomeFragment.run();

                        // Refresh welcome message with updated data
                        refreshExpenseWelcomeMessage.run();
                    });
                }

            } catch (Exception e) {
                android.util.Log.e("ExpenseBulkService", "Error processing expense operations", e);

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