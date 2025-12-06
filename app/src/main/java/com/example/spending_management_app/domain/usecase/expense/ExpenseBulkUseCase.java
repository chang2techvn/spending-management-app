package com.example.spending_management_app.domain.usecase.expense;

import android.content.Context;

import com.example.spending_management_app.R;
import com.example.spending_management_app.data.local.entity.TransactionEntity;
import com.example.spending_management_app.domain.repository.ExpenseRepository;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet;
import com.example.spending_management_app.utils.BudgetAmountParser;
import com.example.spending_management_app.utils.CategoryIconHelper;
import com.example.spending_management_app.utils.DateParser;
import com.example.spending_management_app.utils.ExtractorHelper;
import com.example.spending_management_app.utils.ToastHelper;
import com.example.spending_management_app.utils.CurrencyFormatter;
import com.example.spending_management_app.utils.UserSession;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;

public class ExpenseBulkUseCase {

    private final ExpenseRepository expenseRepository;
    private final UserSession userSession;

    public ExpenseBulkUseCase(ExpenseRepository expenseRepository, Context context) {
        this.expenseRepository = expenseRepository;
        this.userSession = UserSession.getInstance(context);
    }

    // ==================== EXPENSE BULK MANAGEMENT ====================

    public void handleExpenseBulkRequest(String text, Context context,
            android.app.Activity activity, List<AiChatBottomSheet.ChatMessage> messages,
            AiChatBottomSheet.ChatAdapter chatAdapter, androidx.recyclerview.widget.RecyclerView messagesRecycler,
            Runnable refreshHomeFragment, Runnable refreshExpenseWelcomeMessage) {

        android.util.Log.d("ExpenseBulkService", "=== handleExpenseBulkRequest START ===");
        android.util.Log.d("ExpenseBulkService", "Input text: [" + text + "]");

        // Add analyzing message
        int analyzingIndex = messages.size();
        messages.add(new AiChatBottomSheet.ChatMessage(context.getString(R.string.processing_request), false, context.getString(R.string.now_label)));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        String lowerText = text.toLowerCase();

        // Parse multiple expense operations from text
        List<ExpenseOperation> operations = parseMultipleExpenseOperations(text);

        android.util.Log.d("ExpenseBulkService", "Parsed operations count: " + operations.size());
        for (int i = 0; i < operations.size(); i++) {
            ExpenseOperation op = operations.get(i);
            android.util.Log.d("ExpenseBulkService", "Operation " + i + ": type=" + op.type + ", identifier=" + op.identifier + ", transactionId=" + op.transactionId);
        }

        if (operations.isEmpty()) {
            android.util.Log.d("ExpenseBulkService", "No operations parsed, showing help message");
            // Unknown command
            activity.runOnUiThread(() -> {
                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                        context.getString(R.string.expense_bulk_unknown_command),
                        false, context.getString(R.string.now_label)));
                chatAdapter.notifyItemChanged(analyzingIndex);
            });
            return;
        }

        // Process all operations
        processExpenseOperations(operations, analyzingIndex, context, activity, messages, chatAdapter, messagesRecycler, refreshHomeFragment, refreshExpenseWelcomeMessage, text);

        android.util.Log.d("ExpenseBulkService", "=== handleExpenseBulkRequest END ===");
    }

    // Helper class for expense operations
    public static class ExpenseOperation {
        String type; // "add", "edit", "delete", "error"
        String description;
        String category;
        long amount;
        Date date;
        int transactionId; // For edit/delete operations (legacy)
        String identifier; // For delete/edit: "id:123", "date:timestamp", "desc:description", or error message

        ExpenseOperation(String type, String description, String category, long amount, Date date) {
            this.type = type;
            this.description = description;
            this.category = category;
            this.amount = amount;
            this.date = date;
            this.transactionId = -1;
            this.identifier = "";
        }

        ExpenseOperation(String type, int transactionId) {
            this.type = type;
            this.transactionId = transactionId;
            this.description = "";
            this.category = "";
            this.amount = 0;
            this.date = new Date();
            this.identifier = "";
        }

        ExpenseOperation(String type, String identifier) {
            this.type = type;
            this.identifier = identifier;
            this.transactionId = -1;
            this.description = "";
            this.category = "";
            this.amount = 0;
            this.date = new Date();
        }

        // New constructor for edit operations with new amount
        ExpenseOperation(String type, String identifier, long newAmount) {
            this.type = type;
            this.identifier = identifier;
            this.transactionId = -1;
            this.description = "";
            this.category = "";
            this.amount = newAmount; // New amount for edit
            this.date = new Date();
        }
    }

    private static List<ExpenseOperation> parseMultipleExpenseOperations(String text) {
        List<ExpenseOperation> operations = new ArrayList<>();
        String lowerText = text.toLowerCase();

        android.util.Log.d("ExpenseBulkService", "=== parseMultipleExpenseOperations START ===");
        android.util.Log.d("ExpenseBulkService", "Input text: [" + text + "]");
        android.util.Log.d("ExpenseBulkService", "Lower text: [" + lowerText + "]");

        // Determine operation type
        String operationType = "add"; // default
        if (lowerText.contains("xóa") || lowerText.contains("xoá") || lowerText.contains("xoa") ||
            lowerText.contains("delete") || lowerText.contains("remove")) {
            operationType = "delete";
            android.util.Log.d("ExpenseBulkService", "Detected DELETE operation");
        } else if (lowerText.contains("sửa") || lowerText.contains("thay đổi") || lowerText.contains("cập nhật") ||
                   lowerText.contains("edit") || lowerText.contains("update") || lowerText.contains("change") || lowerText.contains("modify")) {
            operationType = "edit";
            android.util.Log.d("ExpenseBulkService", "Detected EDIT operation");
        } else {
            android.util.Log.d("ExpenseBulkService", "Default ADD operation");
        }

        // For edit/delete, try to extract transaction ID or description or date
        if (operationType.equals("delete") || operationType.equals("edit")) {
            android.util.Log.d("ExpenseBulkService", "Processing delete/edit operation");

            // Try to find ID pattern like "#123", "ID 123", "id:123"
            Pattern idPattern = Pattern.compile("(?:#|id[:\\s]+)(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = idPattern.matcher(lowerText);

            while (matcher.find()) {
                int transactionId = Integer.parseInt(matcher.group(1));
                operations.add(new ExpenseOperation(operationType, transactionId));
                android.util.Log.d("ExpenseBulkService", "Found ID pattern: " + transactionId);
            }

            // If no ID found, try to extract date, month, or year for bulk operations
            if (operations.isEmpty()) {
                android.util.Log.d("ExpenseBulkService", "No ID found, trying to extract date/month/year");

                // Priority order: year -> month -> date (most specific to least specific)
                // This prevents "tháng này" from being interpreted as "ngày này"

                // First check if text contains year-related keywords
                boolean hasYearKeywords = lowerText.contains("năm") || lowerText.contains("nam") || lowerText.contains("year");
                android.util.Log.d("ExpenseBulkService", "Has year keywords: " + hasYearKeywords);

                if (hasYearKeywords) {
                    // Try to extract year
                    int year = DateParser.extractYear(lowerText);
                    android.util.Log.d("ExpenseBulkService", "DateParser.extractYear result: " + year);

                    if (year > 0) {
                        android.util.Log.d("ExpenseBulkService", "Valid year extracted: " + year);

                        // Allow operations on any year (past, present, future)
                        // Add operation with year
                        operations.add(new ExpenseOperation(operationType, "year:" + year));
                        android.util.Log.d("ExpenseBulkService", "Created year operation: year:" + year);
                    } else {
                        android.util.Log.d("ExpenseBulkService", "No valid year extracted");
                    }
                }

                // If no year found or no year keywords, check for month keywords
                if (operations.isEmpty()) {
                    boolean hasMonthKeywords = lowerText.contains("tháng") || lowerText.contains("thang") || lowerText.contains("month");
                    android.util.Log.d("ExpenseBulkService", "Has month keywords: " + hasMonthKeywords);

                    if (hasMonthKeywords) {
                        // Try to extract month/year
                        int[] monthYear = DateParser.extractMonthYear(lowerText);
                        android.util.Log.d("ExpenseBulkService", "DateParser.extractMonthYear result: " + monthYear[0] + "/" + monthYear[1]);

                        if (monthYear != null && monthYear.length == 2) {
                            android.util.Log.d("ExpenseBulkService", "Valid month/year extracted: " + monthYear[0] + "/" + monthYear[1]);

                            // Allow operations on any month (past, present, future)
                            // Add operation with month-year
                            operations.add(new ExpenseOperation(operationType, "month:" + monthYear[0] + "-" + monthYear[1]));
                            android.util.Log.d("ExpenseBulkService", "Created month operation: month:" + monthYear[0] + "-" + monthYear[1]);
                        } else {
                            android.util.Log.d("ExpenseBulkService", "No valid month/year extracted");
                        }
                    }
                }

                // If no month found or no month keywords, try to extract specific date
                if (operations.isEmpty()) {
                    Date operationDate = DateParser.extractDateFromText(lowerText);
                    android.util.Log.d("ExpenseBulkService", "DateParser.extractDateFromText result: " + operationDate);

                    if (operationDate != null) {
                        android.util.Log.d("ExpenseBulkService", "Valid date extracted: " + operationDate);

                        // Allow operations on any date (past, present, future)
                        // Add operation with date
                        operations.add(new ExpenseOperation(operationType, "date:" + operationDate.getTime()));
                        android.util.Log.d("ExpenseBulkService", "Created date operation: date:" + operationDate.getTime());
                    } else {
                        android.util.Log.d("ExpenseBulkService", "No valid date extracted");
                    }
                }
            }

            // For edit operations, try to extract new amount
            if (operationType.equals("edit") && !operations.isEmpty()) {
                android.util.Log.d("ExpenseBulkService", "Processing edit operation - trying to extract new amount");

                // Extract new amount from text (look for patterns like "thành 50k", "là 30k", etc.)
                long newAmount = BudgetAmountParser.extractBudgetAmount(text);
                android.util.Log.d("ExpenseBulkService", "Extracted new amount for edit: " + newAmount);

                if (newAmount > 0) {
                    // Update the last operation with new amount
                    ExpenseOperation lastOp = operations.get(operations.size() - 1);
                    operations.set(operations.size() - 1, new ExpenseOperation(lastOp.type, lastOp.identifier, newAmount));
                    android.util.Log.d("ExpenseBulkService", "Updated edit operation with new amount: " + newAmount);
                }
            }

            // If still no operations but user said delete/edit, try to find by description
            if (operations.isEmpty()) {
                android.util.Log.d("ExpenseBulkService", "No date found, trying description extraction");
                // Extract potential description from text (remove keywords)
                String description = lowerText.replaceAll("(xóa|xoa|xoá|sửa|thay đổi|cập nhật|chi tiêu|giao dịch|tất cả|toàn bộ|thành|thanh|delete|remove|edit|update|change|modify|expense|transaction|all|everything|to|into)", "").trim();
                android.util.Log.d("ExpenseBulkService", "Extracted description: [" + description + "]");
                if (!description.isEmpty() && description.length() > 2) {
                    operations.add(new ExpenseOperation(operationType, "desc:" + description));
                    android.util.Log.d("ExpenseBulkService", "Created description operation: desc:" + description);
                } else {
                    android.util.Log.d("ExpenseBulkService", "Description too short or empty");
                }
            }

            // If no operations found but user said delete/edit, inform them
            if (operations.isEmpty()) {
                android.util.Log.d("ExpenseBulkService", "No operations created for delete/edit");
                // Return empty - will show error message
                return operations;
            }
        }

        // For add operations, parse expenses from text
        if (operationType.equals("add")) {
            android.util.Log.d("ExpenseBulkService", "Processing add operations");
            operations = parseExpensesFromText(text);
        }

        android.util.Log.d("ExpenseBulkService", "Final operations count: " + operations.size());
        android.util.Log.d("ExpenseBulkService", "=== parseMultipleExpenseOperations END ===");

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

            // Validate date for add operations - allow past dates but not too far back (max 1 year)
            if (expenseDate != null) {
                Calendar now = Calendar.getInstance();
                Calendar expenseCal = Calendar.getInstance();
                expenseCal.setTime(expenseDate);

                // Allow future dates without restriction
                if (expenseDate.after(now.getTime())) {
                    android.util.Log.d("ExpenseBulkService", "Future date allowed: " + expenseDate);
                } else {
                    // For past dates, check if not more than 1 year ago
                    now.add(Calendar.YEAR, -1);
                    if (expenseDate.before(now.getTime())) {
                        android.util.Log.d("ExpenseBulkService", "Date too far in past, using today: " + expenseDate);
                        expenseDate = new Date(); // Use today for dates more than 1 year ago
                    }
                    now.add(Calendar.YEAR, 1); // Reset calendar
                }
            } else {
                expenseDate = new Date(); // Default to today if no date found
            }

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
            Runnable refreshHomeFragment, Runnable refreshExpenseWelcomeMessage, String text) {

        android.util.Log.d("ExpenseBulkService", "=== processExpenseOperations START ===");
        android.util.Log.d("ExpenseBulkService", "Operations to process: " + operations.size());

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                StringBuilder resultMessage = new StringBuilder();
                int[] counts = {0, 0}; // success, failure

                for (ExpenseOperation op : operations) {
                    int userId = userSession.getCurrentUserId();
                    android.util.Log.d("ExpenseBulkService", "Processing operation: type=" + op.type + ", identifier=" + op.identifier);
                    try {
                        if (op.type.equals("error")) {
                            // Handle error messages
                            resultMessage.append(context.getString(R.string.expense_bulk_error_prefix)).append(op.identifier).append("\n");
                            counts[1]++;
                            android.util.Log.d("ExpenseBulkService", "Error operation processed: " + op.identifier);

                        } else if (op.type.equals("delete")) {
                            android.util.Log.d("ExpenseBulkService", "Processing delete operation");
                            // Handle different types of delete operations
                            if (op.transactionId > 0) {
                                // Delete by ID (legacy)
                                android.util.Log.d("ExpenseBulkService", "Deleting by ID: " + op.transactionId);
                                /* userId declared at top of loop */
                                TransactionEntity transaction = expenseRepository.getTransactionById(userId, op.transactionId);
                                if (transaction != null) {
                                    android.util.Log.d("ExpenseBulkService", "Found transaction to delete: " + transaction.description);
                                    expenseRepository.delete(transaction);
                                    resultMessage.append(String.format(context.getString(R.string.expense_bulk_delete_success), 
                                            transaction.description, CurrencyFormatter.formatCurrency(context, Math.abs(transaction.amount))))
                                            .append("\n");
                                    counts[0]++;
                                    android.util.Log.d("ExpenseBulkService", "Successfully deleted transaction by ID");
                                } else {
                                    resultMessage.append(String.format(context.getString(R.string.expense_bulk_not_found_by_id), op.transactionId)).append("\n");
                                    counts[1]++;
                                    android.util.Log.d("ExpenseBulkService", "Transaction not found by ID: " + op.transactionId);
                                }
                            } else if (op.identifier.startsWith("date:")) {
                                // Delete all transactions on a specific date
                                long timestamp = Long.parseLong(op.identifier.substring(5));
                                Date targetDate = new Date(timestamp);
                                android.util.Log.d("ExpenseBulkService", "Deleting all transactions on date: " + targetDate);

                                /* userId declared at top of loop */
                                List<TransactionEntity> transactionsOnDate = expenseRepository.getTransactionsByDate(userId, targetDate);
                                android.util.Log.d("ExpenseBulkService", "Found " + transactionsOnDate.size() + " transactions on date");
                                if (!transactionsOnDate.isEmpty()) {
                                    int deletedCount = 0;
                                    for (TransactionEntity transaction : transactionsOnDate) {
                                        android.util.Log.d("ExpenseBulkService", "Deleting transaction: " + transaction.description);
                                        expenseRepository.delete(transaction);
                                        deletedCount++;
                                    }
                                    resultMessage.append(String.format(context.getString(R.string.expense_bulk_delete_date_success), 
                                            deletedCount, new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(targetDate)))
                                            .append("\n");
                                    counts[0] += deletedCount;
                                    android.util.Log.d("ExpenseBulkService", "Successfully deleted " + deletedCount + " transactions on date");
                                } else {
                                    resultMessage.append(String.format(context.getString(R.string.expense_bulk_no_expenses_on_date), 
                                            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(targetDate)))
                                            .append("\n");
                                    counts[1]++;
                                    android.util.Log.d("ExpenseBulkService", "No transactions found on date");
                                }
                            } else if (op.identifier.startsWith("month:")) {
                                // Delete all transactions in a specific month
                                String monthYearStr = op.identifier.substring(6); // Remove "month:"
                                String[] parts = monthYearStr.split("-");
                                int month = Integer.parseInt(parts[0]);
                                int year = Integer.parseInt(parts[1]);
                                android.util.Log.d("ExpenseBulkService", "Deleting all transactions in month: " + month + "/" + year);

                                // Calculate start and end dates of the month
                                Calendar cal = Calendar.getInstance();
                                cal.set(year, month - 1, 1, 0, 0, 0); // Month is 0-based
                                cal.set(Calendar.MILLISECOND, 0);
                                Date startOfMonth = cal.getTime();

                                cal.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
                                cal.set(Calendar.MILLISECOND, 999);
                                Date endOfMonth = cal.getTime();

                                android.util.Log.d("ExpenseBulkService", "Month range: " + startOfMonth + " to " + endOfMonth);

                                /* userId declared at top of loop */
                                List<TransactionEntity> transactionsInMonth = expenseRepository.getTransactionsByDateRange(userId, startOfMonth, endOfMonth);
                                android.util.Log.d("ExpenseBulkService", "Found " + transactionsInMonth.size() + " transactions in month");
                                if (!transactionsInMonth.isEmpty()) {
                                    int deletedCount = 0;
                                    for (TransactionEntity transaction : transactionsInMonth) {
                                        android.util.Log.d("ExpenseBulkService", "Deleting transaction: " + transaction.description);
                                        expenseRepository.delete(transaction);
                                        deletedCount++;
                                    }
                                    resultMessage.append(String.format(context.getString(R.string.expense_bulk_delete_month_success), 
                                            deletedCount, month, year))
                                            .append("\n");
                                    counts[0] += deletedCount;
                                    android.util.Log.d("ExpenseBulkService", "Successfully deleted " + deletedCount + " transactions in month");
                                } else {
                                    resultMessage.append(String.format(context.getString(R.string.expense_bulk_no_expenses_in_month), 
                                            month, year))
                                            .append("\n");
                                    counts[1]++;
                                    android.util.Log.d("ExpenseBulkService", "No transactions found in month");
                                }
                            } else if (op.identifier.startsWith("year:")) {
                                // Delete all transactions in a specific year
                                int year = Integer.parseInt(op.identifier.substring(5)); // Remove "year:"
                                android.util.Log.d("ExpenseBulkService", "Deleting all transactions in year: " + year);

                                // Calculate start and end dates of the year
                                Calendar cal = Calendar.getInstance();
                                cal.set(year, 0, 1, 0, 0, 0); // January 1st
                                cal.set(Calendar.MILLISECOND, 0);
                                Date startOfYear = cal.getTime();

                                cal.set(year, 11, 31, 23, 59, 59); // December 31st
                                cal.set(Calendar.MILLISECOND, 999);
                                Date endOfYear = cal.getTime();

                                android.util.Log.d("ExpenseBulkService", "Year range: " + startOfYear + " to " + endOfYear);

                                /* userId declared at top of loop */
                                List<TransactionEntity> transactionsInYear = expenseRepository.getTransactionsByDateRange(userId, startOfYear, endOfYear);
                                android.util.Log.d("ExpenseBulkService", "Found " + transactionsInYear.size() + " transactions in year");
                                if (!transactionsInYear.isEmpty()) {
                                    int deletedCount = 0;
                                    for (TransactionEntity transaction : transactionsInYear) {
                                        android.util.Log.d("ExpenseBulkService", "Deleting transaction: " + transaction.description);
                                        expenseRepository.delete(transaction);
                                        deletedCount++;
                                    }
                                    resultMessage.append(String.format(context.getString(R.string.expense_bulk_delete_year_success), 
                                            deletedCount, year))
                                            .append("\n");
                                    counts[0] += deletedCount;
                                    android.util.Log.d("ExpenseBulkService", "Successfully deleted " + deletedCount + " transactions in year");
                                } else {
                                    resultMessage.append(String.format(context.getString(R.string.expense_bulk_no_expenses_in_year), 
                                            year))
                                            .append("\n");
                                    counts[1]++;
                                    android.util.Log.d("ExpenseBulkService", "No transactions found in year");
                                }
                            } else if (op.identifier.startsWith("desc:")) {
                                // Delete by description (find most recent matching transaction)
                                String searchDesc = op.identifier.substring(5).toLowerCase();
                                android.util.Log.d("ExpenseBulkService", "Deleting by description: " + searchDesc);
                                /* userId declared at top of loop */
                                List<TransactionEntity> allTransactions = expenseRepository.getAllTransactions(userId);
                                android.util.Log.d("ExpenseBulkService", "Total transactions in DB: " + allTransactions.size());

                                TransactionEntity foundTransaction = null;
                                for (TransactionEntity transaction : allTransactions) {
                                    if (transaction.description.toLowerCase().contains(searchDesc)) {
                                        foundTransaction = transaction;
                                        android.util.Log.d("ExpenseBulkService", "Found matching transaction: " + transaction.description);
                                        break; // Take the first (most recent) match
                                    }
                                }

                                if (foundTransaction != null) {
                                    android.util.Log.d("ExpenseBulkService", "Deleting found transaction: " + foundTransaction.description);
                                    expenseRepository.delete(foundTransaction);
                                    resultMessage.append(String.format(context.getString(R.string.expense_bulk_delete_by_desc_success), 
                                            foundTransaction.description, CurrencyFormatter.formatCurrency(context, Math.abs(foundTransaction.amount))))
                                            .append("\n");
                                    counts[0]++;
                                    android.util.Log.d("ExpenseBulkService", "Successfully deleted transaction by description");
                                } else {
                                    resultMessage.append(String.format(context.getString(R.string.expense_bulk_not_found_by_desc), searchDesc)).append("\n");
                                    counts[1]++;
                                    android.util.Log.d("ExpenseBulkService", "No transaction found by description: " + searchDesc);
                                }
                            } else {
                                resultMessage.append(context.getString(R.string.expense_bulk_cannot_determine_delete)).append("\n");
                                counts[1]++;
                                android.util.Log.d("ExpenseBulkService", "Unable to determine what to delete");
                            }

                        } else if (op.type.equals("edit")) {
                            android.util.Log.d("ExpenseBulkService", "Processing edit operation");
                            processEditOperation(op, resultMessage, counts, context, text);
                        } else if (op.type.equals("add")) {
                            android.util.Log.d("ExpenseBulkService", "Processing add operation: " + op.description);
                            // Add new transaction
                            TransactionEntity newTransaction = new TransactionEntity(
                                    op.description,
                                    op.category,
                                    -Math.abs(op.amount), // Expense is negative
                                    op.date,
                                    "expense"
                            );
                            newTransaction.setUserId(userSession.getCurrentUserId());

                            expenseRepository.insert(newTransaction);
                            android.util.Log.d("ExpenseBulkService", "Successfully added transaction: " + op.description + " for userId: " + newTransaction.getUserId());

                            String icon = CategoryIconHelper.getIconEmoji(op.category);
                            resultMessage.append(String.format(context.getString(R.string.expense_bulk_add_success), 
                                    icon, op.description, CurrencyFormatter.formatCurrency(context, op.amount), op.category))
                                    .append("\n");
                            counts[0]++;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ExpenseBulkService", "Error processing expense operation", e);
                        resultMessage.append(String.format(context.getString(R.string.expense_bulk_processing_error_op), 
                                op.description != null ? op.description : op.identifier)).append("\n");
                        counts[1]++;
                    }
                }

                // Add summary
                if (counts[1] > 0) {
                    resultMessage.append(String.format(context.getString(R.string.expense_bulk_result_with_failures), counts[0], counts[1]));
                } else {
                    resultMessage.append(String.format(context.getString(R.string.expense_bulk_result_summary), counts[0]));
                }

                String finalMessage = resultMessage.toString();
                android.util.Log.d("ExpenseBulkService", "Final result message: " + finalMessage);

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        android.util.Log.d("ExpenseBulkService", "Updating UI with result");
                        messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(finalMessage, false, context.getString(R.string.now_label)));
                        chatAdapter.notifyItemChanged(analyzingIndex);

                        // Show toast based on result
                        if (counts[1] > 0) {
                            if (counts[0] > 0) {
                                ToastHelper.showErrorToast(activity, String.format(activity.getString(R.string.partial_success_toast), counts[0], counts[1]));
                            } else {
                                ToastHelper.showErrorToast(activity, String.format(activity.getString(R.string.complete_failure_toast), counts[1], activity.getString(R.string.transactions_label)));
                            }
                        } else {
                            // Check if this was a delete operation
                            boolean hasDeleteOperations = operations.stream().anyMatch(op -> "delete".equals(op.type));
                            if (hasDeleteOperations) {
                                ToastHelper.showToastOnTop(activity, String.format(activity.getString(R.string.expenses_deleted_toast), counts[0]));
                                android.util.Log.d("ExpenseBulkService", "Showing delete success toast");
                            } else {
                                ToastHelper.showToastOnTop(activity, String.format(activity.getString(R.string.expenses_added_toast), counts[0]));
                                android.util.Log.d("ExpenseBulkService", "Showing add success toast");
                            }
                        }

                        android.util.Log.d("ExpenseBulkService", "Calling refreshHomeFragment");
                        refreshHomeFragment.run();

                        // Refresh welcome message with updated data
                        android.util.Log.d("ExpenseBulkService", "Calling refreshExpenseWelcomeMessage");
                        refreshExpenseWelcomeMessage.run();
                    });
                }

            } catch (Exception e) {
                android.util.Log.e("ExpenseBulkService", "Error processing expense operations", e);

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(
                                context.getString(R.string.expense_bulk_processing_error),
                                false, context.getString(R.string.now_label)));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                    });
                }
            }
        });

        android.util.Log.d("ExpenseBulkService", "=== processExpenseOperations END ===");
    }

    private void processEditOperation(ExpenseOperation op, StringBuilder resultMessage, int[] counts, Context context, String text) {
        android.util.Log.d("ExpenseBulkService", "=== processEditOperation START ===");
        android.util.Log.d("ExpenseBulkService", "Edit operation identifier: " + op.identifier);

        try {
            TransactionEntity transactionToEdit = null;
            int userId = userSession.getCurrentUserId();

            // Find transaction to edit
            if (op.transactionId > 0) {
                // Edit by ID
                android.util.Log.d("ExpenseBulkService", "Editing by ID: " + op.transactionId);
                transactionToEdit = expenseRepository.getTransactionById(userId, op.transactionId);
            } else if (op.identifier.startsWith("date:")) {
                // Edit most recent transaction on date
                long timestamp = Long.parseLong(op.identifier.substring(5));
                Date targetDate = new Date(timestamp);
                android.util.Log.d("ExpenseBulkService", "Editing most recent transaction on date: " + targetDate);
                List<TransactionEntity> transactionsOnDate = expenseRepository.getTransactionsByDate(userId, targetDate);
                if (!transactionsOnDate.isEmpty()) {
                    // Get most recent transaction
                    transactionToEdit = transactionsOnDate.get(0);
                    for (TransactionEntity t : transactionsOnDate) {
                        if (t.date.after(transactionToEdit.date)) {
                            transactionToEdit = t;
                        }
                    }
                }
            } else if (op.identifier.startsWith("month:")) {
                // Edit most recent transaction in month
                String monthYearStr = op.identifier.substring(6); // Remove "month:"
                String[] parts = monthYearStr.split("-");
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt(parts[1]);
                android.util.Log.d("ExpenseBulkService", "Editing most recent transaction in month: " + month + "/" + year);

                // Calculate start and end dates of the month
                Calendar cal = Calendar.getInstance();
                cal.set(year, month - 1, 1, 0, 0, 0); // Month is 0-based
                cal.set(Calendar.MILLISECOND, 0);
                Date startOfMonth = cal.getTime();

                cal.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
                cal.set(Calendar.MILLISECOND, 999);
                Date endOfMonth = cal.getTime();

                List<TransactionEntity> transactionsInMonth = expenseRepository.getTransactionsByDateRange(userId, startOfMonth, endOfMonth);
                if (!transactionsInMonth.isEmpty()) {
                    // Get most recent transaction
                    transactionToEdit = transactionsInMonth.get(0);
                    for (TransactionEntity t : transactionsInMonth) {
                        if (t.date.after(transactionToEdit.date)) {
                            transactionToEdit = t;
                        }
                    }
                }
            } else if (op.identifier.startsWith("year:")) {
                // Edit most recent transaction in year
                int year = Integer.parseInt(op.identifier.substring(5)); // Remove "year:"
                android.util.Log.d("ExpenseBulkService", "Editing most recent transaction in year: " + year);

                // Calculate start and end dates of the year
                Calendar cal = Calendar.getInstance();
                cal.set(year, 0, 1, 0, 0, 0); // January 1st
                cal.set(Calendar.MILLISECOND, 0);
                Date startOfYear = cal.getTime();

                cal.set(year, 11, 31, 23, 59, 59); // December 31st
                cal.set(Calendar.MILLISECOND, 999);
                Date endOfYear = cal.getTime();

                List<TransactionEntity> transactionsInYear = expenseRepository.getTransactionsByDateRange(userId, startOfYear, endOfYear);
                if (!transactionsInYear.isEmpty()) {
                    // Get most recent transaction
                    transactionToEdit = transactionsInYear.get(0);
                    for (TransactionEntity t : transactionsInYear) {
                        if (t.date.after(transactionToEdit.date)) {
                            transactionToEdit = t;
                        }
                    }
                }
            } else if (op.identifier.startsWith("desc:")) {
                // Edit by description (find most recent matching transaction)
                String searchDesc = op.identifier.substring(5).toLowerCase();
                android.util.Log.d("ExpenseBulkService", "Editing by description: " + searchDesc);
                List<TransactionEntity> allTransactions = expenseRepository.getAllTransactions(userId);
                for (TransactionEntity transaction : allTransactions) {
                    if (transaction.description.toLowerCase().contains(searchDesc)) {
                        transactionToEdit = transaction;
                        android.util.Log.d("ExpenseBulkService", "Found matching transaction: " + transaction.description);
                        break; // Take the first (most recent) match
                    }
                }
            } else {
                // Try to find by description and date from the original text
                // This handles cases like "sửa cafe hôm nay thành 50k"
                android.util.Log.d("ExpenseBulkService", "Trying to find by description and date from original text");

                String originalText = text.toLowerCase();
                String description = "";
                Date targetDate = null;

                // Extract description (remove edit keywords and amounts)
                description = originalText.replaceAll("(sửa|thay đổi|cập nhật|thành|là|được|đổi|chỉnh)", "").trim();

                // Try to extract date from original text
                targetDate = DateParser.extractDateFromText(text);

                android.util.Log.d("ExpenseBulkService", "Extracted description: " + description + ", date: " + targetDate);

                if (!description.isEmpty() && targetDate != null) {
                    // Find transactions on the target date that match the description
                    List<TransactionEntity> transactionsOnDate = expenseRepository.getTransactionsByDate(userId, targetDate);
                    android.util.Log.d("ExpenseBulkService", "Found " + transactionsOnDate.size() + " transactions on date");

                    for (TransactionEntity transaction : transactionsOnDate) {
                        if (transaction.description.toLowerCase().contains(description.toLowerCase())) {
                            transactionToEdit = transaction;
                            android.util.Log.d("ExpenseBulkService", "Found matching transaction by desc+date: " + transaction.description);
                            break; // Take the first match
                        }
                    }
                }
            }

            if (transactionToEdit == null) {
                resultMessage.append(context.getString(R.string.expense_bulk_edit_not_found)).append("\n");
                counts[1]++;
                android.util.Log.d("ExpenseBulkService", "No transaction found to edit");
                return;
            }

            android.util.Log.d("ExpenseBulkService", "Found transaction to edit: " + transactionToEdit.description);

            // Check if we have a new amount to update
            if (op.amount > 0) {
                android.util.Log.d("ExpenseBulkService", "Updating transaction amount from " + transactionToEdit.amount + " to " + (-Math.abs(op.amount)));

                // Update the transaction with new amount (expense is negative)
                TransactionEntity updatedTransaction = new TransactionEntity(
                        transactionToEdit.description,
                        transactionToEdit.category,
                        -Math.abs(op.amount), // Ensure it's negative for expense
                        transactionToEdit.date,
                        transactionToEdit.type
                );
                updatedTransaction.id = transactionToEdit.id; // Preserve ID

                expenseRepository.update(updatedTransaction);
                android.util.Log.d("ExpenseBulkService", "Successfully updated transaction amount");

                resultMessage.append(String.format(context.getString(R.string.expense_bulk_edit_success), 
                        transactionToEdit.description, 
                        CurrencyFormatter.formatCurrency(context, Math.abs(transactionToEdit.amount)),
                        CurrencyFormatter.formatCurrency(context, op.amount)))
                        .append("\n");
                counts[0]++;
            } else {
                // No new amount provided - show placeholder message
                resultMessage.append(context.getString(R.string.expense_bulk_edit_not_implemented)).append("\n");
                counts[1]++;
                android.util.Log.d("ExpenseBulkService", "Edit operation not fully implemented - no amount provided");
            }

        } catch (Exception e) {
            android.util.Log.e("ExpenseBulkService", "Error in processEditOperation", e);
            resultMessage.append(context.getString(R.string.expense_bulk_edit_error)).append("\n");
            counts[1]++;
        }

        android.util.Log.d("ExpenseBulkService", "=== processEditOperation END ===");
    }
}