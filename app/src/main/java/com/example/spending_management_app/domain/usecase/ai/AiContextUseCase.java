package com.example.spending_management_app.domain.usecase.ai;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.BuildConfig;
import com.example.spending_management_app.domain.repository.BudgetRepository;
import com.example.spending_management_app.domain.repository.CategoryBudgetRepository;
import com.example.spending_management_app.domain.repository.ExpenseRepository;
import com.example.spending_management_app.data.local.entity.BudgetEntity;
import com.example.spending_management_app.data.local.entity.TransactionEntity;
import com.example.spending_management_app.data.local.entity.CategoryBudgetEntity;
import com.example.spending_management_app.data.remote.api.GeminiApiService;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet;
import com.example.spending_management_app.utils.TextFormatHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiContextUseCase {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryBudgetRepository categoryBudgetRepository;

    public AiContextUseCase(ExpenseRepository expenseRepository, BudgetRepository budgetRepository, CategoryBudgetRepository categoryBudgetRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.categoryBudgetRepository = categoryBudgetRepository;
    }

        // Get comprehensive financial context from database
    public String getFinancialContext(Context context) {
        StringBuilder contextBuilder = new StringBuilder();
        
        try {
            // Get current month date range
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
            Date endOfMonth = cal.getTime();

            // Get all transactions this month
            List<TransactionEntity> monthlyTransactions = expenseRepository.getTransactionsByDateRange(startOfMonth, endOfMonth);

            // Calculate totals
            long totalExpense = 0;
            long totalIncome = 0;
            java.util.Map<String, Long> expensesByCategory = new java.util.HashMap<>();
            java.util.Map<String, Integer> transactionCountByDay = new java.util.HashMap<>();
            
            SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault());
            
            for (TransactionEntity transaction : monthlyTransactions) {
                if ("expense".equals(transaction.type)) {
                    totalExpense += Math.abs(transaction.amount);
                    expensesByCategory.put(transaction.category, 
                        expensesByCategory.getOrDefault(transaction.category, 0L) + Math.abs(transaction.amount));
                } else if ("income".equals(transaction.type)) {
                    totalIncome += transaction.amount;
                }
                
                // Count transactions by day
                String day = dayFormat.format(transaction.date);
                transactionCountByDay.put(day, transactionCountByDay.getOrDefault(day, 0) + 1);
            }

            // Get budget info
            List<BudgetEntity> monthlyBudgets = budgetRepository.getBudgetsByDateRange(startOfMonth, endOfMonth);

            // Build context string
            contextBuilder.append("THÔNG TIN TÀI CHÍNH THÁNG NÀY:\n");
            contextBuilder.append("- Tổng thu nhập: ").append(String.format(Locale.getDefault(), "%,d", totalIncome)).append(" VND\n");
            contextBuilder.append("- Tổng chi tiêu: ").append(String.format(Locale.getDefault(), "%,d", totalExpense)).append(" VND\n");
            contextBuilder.append("- Số dư ước tính: ").append(String.format(Locale.getDefault(), "%,d", (totalIncome - totalExpense))).append(" VND\n");
            
            if (!monthlyBudgets.isEmpty()) {
                BudgetEntity budget = monthlyBudgets.get(0);
                long remaining = budget.getMonthlyLimit() - totalExpense;
                contextBuilder.append("- Ngân sách tháng: ").append(String.format(Locale.getDefault(), "%,d", budget.getMonthlyLimit())).append(" VND\n");
                contextBuilder.append("- Còn lại: ").append(String.format(Locale.getDefault(), "%,d", remaining)).append(" VND\n");
                contextBuilder.append("- Tỷ lệ sử dụng: ").append(String.format("%.1f", (double)totalExpense/budget.getMonthlyLimit()*100)).append("%\n");
            }
            
            contextBuilder.append("\nCHI TIÊU THEO DANH MỤC:\n");
            for (java.util.Map.Entry<String, Long> entry : expensesByCategory.entrySet()) {
                double percentage = totalExpense > 0 ? (double)entry.getValue()/totalExpense*100 : 0;
                contextBuilder.append("- ").append(entry.getKey()).append(": ")
                       .append(String.format(Locale.getDefault(), "%,d", entry.getValue()))
                       .append(" VND (").append(String.format("%.1f", percentage)).append("%)\n");
            }
            
            contextBuilder.append("\nGAO DỊCH GẦN ĐÂY:\n");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            List<TransactionEntity> recentTransactions = monthlyTransactions.stream()
                    .sorted((t1, t2) -> t2.date.compareTo(t1.date))
                    .limit(10)
                    .collect(java.util.stream.Collectors.toList());
            
            for (TransactionEntity t : recentTransactions) {
                contextBuilder.append("- ").append(dateFormat.format(t.date)).append(": ")
                       .append(t.description).append(" (").append(t.category).append(") - ")
                       .append(String.format(Locale.getDefault(), "%,d", Math.abs(t.amount))).append(" VND\n");
            }

        } catch (Exception e) {
            contextBuilder.append("Lỗi khi truy xuất dữ liệu tài chính: ").append(e.getMessage());
        }
        
        return contextBuilder.toString();
    }

    // Send prompt to AI with financial context
    public static void sendPromptToAIWithContext(String userQuery, String financialContext, 
            android.app.Activity activity, List<AiChatBottomSheet.ChatMessage> messages, 
            AiChatBottomSheet.ChatAdapter chatAdapter, RecyclerView messagesRecycler, 
            TextToSpeech textToSpeech, Runnable updateNetworkStatus) {
        // Add temporary "Đang phân tích..." message
        int analyzingIndex = messages.size();
        messages.add(new AiChatBottomSheet.ChatMessage("Đang phân tích dữ liệu tài chính...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        OkHttpClient client = new OkHttpClient();
        
        try {
            JSONObject json = new JSONObject();

            // Get current date for AI context
            java.util.Calendar currentCalendar = java.util.Calendar.getInstance();
            int currentDay = currentCalendar.get(java.util.Calendar.DAY_OF_MONTH);
            int currentMonth = currentCalendar.get(java.util.Calendar.MONTH) + 1;
            int currentYear = currentCalendar.get(java.util.Calendar.YEAR);
            String currentDateInfo = String.format("Hôm nay là ngày %d/%d/%d", currentDay, currentMonth, currentYear);

            // Enhanced system instruction with financial analysis capabilities
            JSONObject systemInstruction = new JSONObject();
            JSONArray systemParts = new JSONArray();
            JSONObject systemPart = new JSONObject();
            
            // Use helper class for financial analysis instruction
            String enhancedInstruction = AiSystemInstructions.getFinancialAnalysisInstruction(
                currentDateInfo, financialContext
            );
            
            systemPart.put("text", enhancedInstruction);
            systemParts.put(systemPart);
            systemInstruction.put("parts", systemParts);
            json.put("system_instruction", systemInstruction);

            // Build conversation history
            JSONArray contents = new JSONArray();
            
            // Add previous messages (excluding the current analyzing message and welcome messages)
            for (int i = 0; i < analyzingIndex; i++) {
                AiChatBottomSheet.ChatMessage msg = messages.get(i);
                // Skip welcome messages or system messages that are not part of conversation
                if (msg.message.startsWith("📊") || msg.message.startsWith("💰") || 
                    msg.message.startsWith("📅") || msg.message.contains("Đang phân tích") ||
                    msg.message.contains("Lỗi") || msg.message.contains("Offline")) {
                    continue;
                }
                
                JSONObject contentObj = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", msg.message);
                parts.put(part);
                contentObj.put("parts", parts);
                contentObj.put("role", msg.isUser ? "user" : "model");
                contents.put(contentObj);
            }
            
            // Add current user query
            JSONObject userContent = new JSONObject();
            JSONArray userParts = new JSONArray();
            JSONObject userPart = new JSONObject();
            userPart.put("text", userQuery);
            userParts.put(userPart);
            userContent.put("parts", userParts);
            userContent.put("role", "user");
            contents.put(userContent);
            
            json.put("contents", contents);

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    activity.runOnUiThread(() -> {
                        messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage("Lỗi kết nối AI.", false, "Bây giờ"));
                        chatAdapter.notifyItemChanged(analyzingIndex);
                        updateNetworkStatus.run();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray candidates = jsonResponse.getJSONArray("candidates");
                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONObject content = candidate.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            String aiText = parts.getJSONObject(0).getString("text").trim();

                            // Format markdown text để dễ đọc
                            String formattedText = TextFormatHelper.formatMarkdownText(aiText);

                            activity.runOnUiThread(() -> {
                                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(formattedText, false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                                textToSpeech.speak(formattedText, TextToSpeech.QUEUE_FLUSH, null, null);
                                updateNetworkStatus.run();
                            });
                        } catch (Exception e) {
                            activity.runOnUiThread(() -> {
                                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage("Lỗi xử lý phản hồi AI.", false, "Bây giờ"));
                                chatAdapter.notifyItemChanged(analyzingIndex);
                                updateNetworkStatus.run();
                            });
                        }
                    } else {
                        activity.runOnUiThread(() -> {
                            messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage("Lỗi từ AI: " + response.code(), false, "Bây giờ"));
                            chatAdapter.notifyItemChanged(analyzingIndex);
                            updateNetworkStatus.run();
                        });
                    }
                }
            });
        } catch (Exception e) {
            messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage("Lỗi gửi tin nhắn.", false, "Bây giờ"));
            chatAdapter.notifyItemChanged(analyzingIndex);
        }
    }

    
    // Get comprehensive budget context from database
    public String getBudgetContext(Context context) {
        StringBuilder contextBuilder = new StringBuilder();
        
        try {
            // Get all budgets (last 12 months)
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -12);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date twelveMonthsAgo = cal.getTime();
            
            cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 6); // Include 6 months in future
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            Date sixMonthsLater = cal.getTime();
            
            List<BudgetEntity> allBudgets = budgetRepository
                    .getBudgetsByDateRangeOrdered(twelveMonthsAgo, sixMonthsLater);
            
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
            
            contextBuilder.append("THÔNG TIN NGÂN SÁCH:\n");
            
            if (allBudgets != null && !allBudgets.isEmpty()) {
                // Group by month
                java.util.Map<String, BudgetEntity> budgetsByMonth = new java.util.HashMap<>();
                for (BudgetEntity budget : allBudgets) {
                    String monthKey = monthYearFormat.format(budget.date);
                    if (!budgetsByMonth.containsKey(monthKey) || 
                        budget.date.after(budgetsByMonth.get(monthKey).date)) {
                        budgetsByMonth.put(monthKey, budget);
                    }
                }
                
                // Sort months
                java.util.List<String> sortedMonths = new java.util.ArrayList<>(budgetsByMonth.keySet());
                java.util.Collections.sort(sortedMonths);
                
                // Calculate current month
                Calendar currentCal = Calendar.getInstance();
                String currentMonth = monthYearFormat.format(currentCal.getTime());
                
                // List all budgets
                contextBuilder.append("\nDanh sách ngân sách theo tháng:\n");
                for (String month : sortedMonths) {
                    BudgetEntity budget = budgetsByMonth.get(month);
                    String formattedAmount = String.format(Locale.getDefault(), "%,d", budget.monthlyLimit);
                    
                    String marker = month.equals(currentMonth) ? " (Tháng hiện tại)" : "";
                    contextBuilder.append("- Tháng ").append(month).append(marker).append(": ")
                           .append(formattedAmount).append(" VND\n");
                }
                
                // Calculate statistics
                long totalBudget = 0;
                long maxBudget = Long.MIN_VALUE;
                long minBudget = Long.MAX_VALUE;
                String maxMonth = "";
                String minMonth = "";
                
                for (String month : sortedMonths) {
                    BudgetEntity budget = budgetsByMonth.get(month);
                    totalBudget += budget.monthlyLimit;
                    
                    if (budget.monthlyLimit > maxBudget) {
                        maxBudget = budget.monthlyLimit;
                        maxMonth = month;
                    }
                    
                    if (budget.monthlyLimit < minBudget) {
                        minBudget = budget.monthlyLimit;
                        minMonth = month;
                    }
                }
                
                long avgBudget = totalBudget / sortedMonths.size();
                
                contextBuilder.append("\nThống kê ngân sách:\n");
                contextBuilder.append("- Tổng số tháng đã thiết lập: ").append(sortedMonths.size()).append("\n");
                contextBuilder.append("- Ngân sách trung bình: ").append(String.format(Locale.getDefault(), "%,d", avgBudget)).append(" VND\n");
                contextBuilder.append("- Ngân sách cao nhất: ").append(String.format(Locale.getDefault(), "%,d", maxBudget))
                       .append(" VND (Tháng ").append(maxMonth).append(")\n");
                contextBuilder.append("- Ngân sách thấp nhất: ").append(String.format(Locale.getDefault(), "%,d", minBudget))
                       .append(" VND (Tháng ").append(minMonth).append(")\n");
                
                // Current month budget status
                if (budgetsByMonth.containsKey(currentMonth)) {
                    BudgetEntity currentBudget = budgetsByMonth.get(currentMonth);
                    contextBuilder.append("\nNgân sách tháng hiện tại: ")
                           .append(String.format(Locale.getDefault(), "%,d", currentBudget.monthlyLimit))
                           .append(" VND\n");
                } else {
                    contextBuilder.append("\nNgân sách tháng hiện tại: Chưa thiết lập\n");
                }
                
            } else {
                contextBuilder.append("Chưa có ngân sách nào được thiết lập.\n");
            }
            
            // ========== THÊM THÔNG TIN NGÂN SÁCH DANH MỤC ==========
            contextBuilder.append("\n");
            contextBuilder.append("NGÂN SÁCH THEO DANH MỤC (THÁNG HIỆN TẠI):\n");
            
            try {
                // Get current month range
                Calendar currentCal = Calendar.getInstance();
                currentCal.set(Calendar.DAY_OF_MONTH, 1);
                currentCal.set(Calendar.HOUR_OF_DAY, 0);
                currentCal.set(Calendar.MINUTE, 0);
                currentCal.set(Calendar.SECOND, 0);
                currentCal.set(Calendar.MILLISECOND, 0);
                Date startOfMonth = currentCal.getTime();
                
                currentCal.set(Calendar.DAY_OF_MONTH, currentCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                currentCal.set(Calendar.HOUR_OF_DAY, 23);
                currentCal.set(Calendar.MINUTE, 59);
                currentCal.set(Calendar.SECOND, 59);
                currentCal.set(Calendar.MILLISECOND, 999);
                Date endOfMonth = currentCal.getTime();
                
                // Get all category budgets for current month
                List<CategoryBudgetEntity> categoryBudgets =
                        categoryBudgetRepository
                                .getAllCategoryBudgetsForMonth(startOfMonth, endOfMonth);
                
                if (categoryBudgets != null && !categoryBudgets.isEmpty()) {
                    // Calculate total
                    long totalCategoryBudget = 0;
                    for (CategoryBudgetEntity budget : categoryBudgets) {
                        totalCategoryBudget += budget.getBudgetAmount();
                    }
                    
                    contextBuilder.append("Tổng ngân sách đã phân bổ: ")
                           .append(String.format(Locale.getDefault(), "%,d", totalCategoryBudget))
                           .append(" VND\n\n");
                    
                    // Sort by amount (highest first)
                    categoryBudgets.sort((a, b) -> Long.compare(b.getBudgetAmount(), a.getBudgetAmount()));
                    
                    // List all category budgets
                    contextBuilder.append("Chi tiết ngân sách từng danh mục:\n");
                    for (CategoryBudgetEntity budget : categoryBudgets) {
                        String formattedAmount = String.format(Locale.getDefault(), "%,d", budget.getBudgetAmount());
                        contextBuilder.append("- ").append(budget.getCategory()).append(": ")
                               .append(formattedAmount).append(" VND\n");
                    }
                    
                    // Calculate percentage for top categories
                    if (totalCategoryBudget > 0) {
                        contextBuilder.append("\nTỷ lệ phân bổ ngân sách:\n");
                        for (int i = 0; i < Math.min(5, categoryBudgets.size()); i++) {
                            CategoryBudgetEntity budget = categoryBudgets.get(i);
                            double percentage = (budget.getBudgetAmount() * 100.0) / totalCategoryBudget;
                            contextBuilder.append("- ").append(budget.getCategory()).append(": ")
                                   .append(String.format(Locale.getDefault(), "%.1f%%", percentage)).append("\n");
                        }
                    }
                } else {
                    contextBuilder.append("Chưa có ngân sách danh mục nào được thiết lập.\n");
                }
            } catch (Exception e) {
                contextBuilder.append("Lỗi khi truy xuất ngân sách danh mục: ").append(e.getMessage()).append("\n");
                Log.e("AiContextService", "Error getting category budget context", e);
            }
            
        } catch (Exception e) {
            contextBuilder.append("Lỗi khi truy xuất dữ liệu ngân sách: ").append(e.getMessage());
            Log.e("AiContextService", "Error getting budget context", e);
        }
        
        return contextBuilder.toString();
    }
    
    // Send prompt to AI with budget context
    /**
     * Send prompt to AI with budget context using GeminiAI service
     * This method uses callback pattern to handle UI updates
     */
    public void sendPromptToAIWithBudgetContext(String userQuery, String budgetContext,
            List<AiChatBottomSheet.ChatMessage> messages, AiChatBottomSheet.ChatAdapter chatAdapter, 
            RecyclerView messagesRecycler, TextToSpeech textToSpeech, Runnable updateNetworkStatus) {
        // Add temporary "Đang phân tích..." message
        int analyzingIndex = messages.size();
        messages.add(new AiChatBottomSheet.ChatMessage("Đang phân tích ngân sách...", false, "Bây giờ"));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        messagesRecycler.smoothScrollToPosition(messages.size() - 1);

        // Use GeminiAI service with callback
        GeminiApiService.sendPromptWithBudgetContext(userQuery, budgetContext, messages, analyzingIndex, new GeminiApiService.AIResponseCallback() {
            @Override
            public void onSuccess(String formattedResponse) {
                // Update UI with AI response
                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(formattedResponse, false, "Bây giờ"));
                chatAdapter.notifyItemChanged(analyzingIndex);
                messagesRecycler.smoothScrollToPosition(messages.size() - 1);
                textToSpeech.speak(formattedResponse, TextToSpeech.QUEUE_FLUSH, null, null);
                updateNetworkStatus.run();
            }

            @Override
            public void onFailure(String errorMessage) {
                // Update UI with error message
                messages.set(analyzingIndex, new AiChatBottomSheet.ChatMessage(errorMessage, false, "Bây giờ"));
                chatAdapter.notifyItemChanged(analyzingIndex);
                updateNetworkStatus.run();
            }
        });
    }
    
}
