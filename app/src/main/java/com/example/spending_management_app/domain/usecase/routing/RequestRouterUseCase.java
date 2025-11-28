package com.example.spending_management_app.domain.usecase.routing;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.repository.BudgetRepositoryImpl;
import com.example.spending_management_app.data.repository.CategoryBudgetRepositoryImpl;
import com.example.spending_management_app.data.repository.ExpenseRepositoryImpl;
import com.example.spending_management_app.domain.repository.BudgetRepository;
import com.example.spending_management_app.domain.repository.CategoryBudgetRepository;
import com.example.spending_management_app.domain.repository.ExpenseRepository;
import com.example.spending_management_app.domain.usecase.ai.AiContextUseCase;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet;
import com.example.spending_management_app.utils.BudgetMessageHelper;
import com.example.spending_management_app.domain.usecase.category.CategoryBudgetUseCase;
import com.example.spending_management_app.utils.ExpenseMessageHelper;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet.ChatMessage;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet.ChatAdapter;
import com.example.spending_management_app.domain.usecase.ai.PromptUseCase;
import com.example.spending_management_app.domain.usecase.expense.ExpenseUseCase;

import java.util.List;
import java.util.concurrent.Executors;

public class RequestRouterUseCase {

    public interface RequestRouterCallback {
        void refreshHomeFragment();
        void refreshExpenseWelcomeMessage();
        void refreshCategoryBudgetWelcomeMessage();
        boolean handleOfflineRequest(String text, boolean isBudgetMode, boolean isCategoryBudgetMode, boolean isExpenseBulkMode);
        void handleBudgetQuery(String text);
        void handleExpenseBulkRequest(String text);
        boolean isNetworkAvailable();
        void updateNetworkStatus();
    }

    public static void routeRequest(String text, Context context, Activity activity, Bundle args,
                                   List<ChatMessage> messages, ChatAdapter chatAdapter,
                                   RecyclerView messagesRecycler, TextToSpeech textToSpeech,
                                   Runnable updateNetworkStatusCallback, RequestRouterCallback callback) {

        // Initialize repositories and use cases
        AppDatabase appDatabase = AppDatabase.getInstance(context);
        ExpenseRepository expenseRepository = new ExpenseRepositoryImpl(appDatabase);
        BudgetRepository budgetRepository = new BudgetRepositoryImpl(appDatabase);
        CategoryBudgetRepository categoryBudgetRepository = new CategoryBudgetRepositoryImpl(appDatabase);
        AiContextUseCase aiContextUseCase = new AiContextUseCase(expenseRepository, budgetRepository, categoryBudgetRepository);
        CategoryBudgetUseCase categoryBudgetUseCase = new CategoryBudgetUseCase(budgetRepository, categoryBudgetRepository);
        ExpenseUseCase expenseUseCase = new ExpenseUseCase(expenseRepository);
        PromptUseCase promptUseCase = new PromptUseCase(expenseUseCase);

        // Check network connectivity first
        boolean isOnline = callback.isNetworkAvailable();

        // Check if this is budget management mode or category budget management mode or expense bulk management mode
        boolean isBudgetMode = args != null && "budget_management".equals(args.getString("mode"));
        boolean isCategoryBudgetMode = args != null && "category_budget_management".equals(args.getString("mode"));
        boolean isExpenseBulkMode = args != null && "expense_bulk_management".equals(args.getString("mode"));

        // Lowercase text for heuristics
        String lowerText = text.toLowerCase();

        // Heuristic routing: prioritize based on content regardless of mode
        // - If has amount and no time, route to category budget (add/update/delete)
        // - If has time indicators, route to expense (AI)
        // - If has amount but no time and no category, will be handled by category budget parser (shows error)
        boolean containsDigit = lowerText.matches(".*\\d+.*");
        boolean hasTimeIndicator = lowerText.contains("hôm") || lowerText.contains("hom") || lowerText.contains("hôm nay") ||
                lowerText.contains("hôm qua") || lowerText.contains("ngày") || lowerText.contains("tháng") || lowerText.contains("năm") ||
                lowerText.contains("sáng") || lowerText.contains("tối") || lowerText.contains("chiều") || lowerText.contains("trưa") ||
                lowerText.contains("today") || lowerText.contains("yesterday") || lowerText.contains("day") || lowerText.contains("month") || lowerText.contains("year") ||
                lowerText.matches(".*\\d{1,2}[/\\-]\\d{1,2}.*");
        boolean explicitBudget = BudgetMessageHelper.isBudgetQuery(text) || lowerText.contains("danh mục") || lowerText.contains("category") || lowerText.contains("ngân sách");

        // Check if this is a query (not an operation)
        boolean isQuery = lowerText.contains("bao nhiêu") || lowerText.contains("là bao nhiêu") || lowerText.contains("hiển thị") ||
                lowerText.contains("xem") || lowerText.contains("tất cả") || lowerText.contains("tat ca") ||
                lowerText.contains("how much") || lowerText.contains("show") || lowerText.contains("view") ||
                lowerText.contains("all") || lowerText.contains("what");

        android.util.Log.d("RequestRouterUseCase", "Heuristic: text=" + text + ", containsDigit=" + containsDigit + ", hasTimeIndicator=" + hasTimeIndicator + ", explicitBudget=" + explicitBudget + ", isQuery=" + isQuery);

        if (containsDigit && !hasTimeIndicator && !isQuery) {
            android.util.Log.d("RequestRouterUseCase", "Routing to CategoryBudgetUseCase for text: " + text);
            categoryBudgetUseCase.handleCategoryBudgetRequest(text, context, activity, messages, chatAdapter, messagesRecycler,
                    () -> callback.refreshHomeFragment(), () -> callback.refreshCategoryBudgetWelcomeMessage());
            return;
        }

        if (containsDigit && hasTimeIndicator) {
            android.util.Log.d("RequestRouterUseCase", "Routing to expense AI for text: " + text);
            promptUseCase.sendPromptToAI(text, activity, messages, chatAdapter, messagesRecycler, textToSpeech, updateNetworkStatusCallback);
            return;
        }

        // Check if user is asking for budget analysis or reports FIRST (before financial queries)
        if (!isBudgetMode && !isCategoryBudgetMode && BudgetMessageHelper.isBudgetQuery(text)) {
            // Get comprehensive budget data from database
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    String budgetContext = aiContextUseCase.getBudgetContext(context);
                    activity.runOnUiThread(() -> {
                        aiContextUseCase.sendPromptToAIWithBudgetContext(context, text, budgetContext, messages, chatAdapter, messagesRecycler, textToSpeech, updateNetworkStatusCallback);
                    });
                } catch (Exception e) {
                    activity.runOnUiThread(() -> {
                        promptUseCase.sendPromptToAI(text, activity, messages, chatAdapter, messagesRecycler, textToSpeech, updateNetworkStatusCallback);
                    });
                }
            });
            return;
        }

        // Check if user is asking for financial analysis or reports (before bulk operations)
        if (!isBudgetMode && ExpenseMessageHelper.isFinancialQuery(text)) {
            // Get comprehensive financial data from database
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    String financialContext = aiContextUseCase.getFinancialContext(context);
                    activity.runOnUiThread(() -> {
                        aiContextUseCase.sendPromptToAIWithContext(text, financialContext, activity, messages, chatAdapter, messagesRecycler, textToSpeech, updateNetworkStatusCallback);
                    });
                } catch (Exception e) {
                    activity.runOnUiThread(() -> {
                        promptUseCase.sendPromptToAI(text, activity, messages, chatAdapter, messagesRecycler, textToSpeech, updateNetworkStatusCallback);
                    });
                }
            });
            return;
        }

        // Also check if this is an expense bulk request based on text content
        // Even if mode is not set, detect expense bulk operations from text
        if (!isExpenseBulkMode) {
            // Check for expense bulk keywords in Vietnamese
            boolean hasVietnameseKeywords = lowerText.contains("xóa") || lowerText.contains("xoá") || lowerText.contains("xoa") ||
                lowerText.contains("thêm chi tiêu") || lowerText.contains("them chi tieu") ||
                lowerText.contains("chi tiêu") || lowerText.contains("chi tieu");

            // Check for expense bulk keywords in English
            boolean hasEnglishKeywords = lowerText.contains("add expense") || lowerText.contains("delete expense") ||
                lowerText.contains("remove expense") || lowerText.contains("spend") || lowerText.contains("expense") ||
                lowerText.contains("spending") || lowerText.contains("cost");

            if (hasVietnameseKeywords || hasEnglishKeywords) {
                // Check if it looks like a bulk operation (contains dates, or multiple items)
                boolean hasBulkIndicators = lowerText.contains("ngày") || lowerText.contains("hôm") || lowerText.contains("tháng") ||
                    lowerText.contains("tất cả") || lowerText.contains("tat ca") ||
                    lowerText.contains("và") || lowerText.contains("cả") ||
                    lowerText.contains("day") || lowerText.contains("today") || lowerText.contains("yesterday") ||
                    lowerText.contains("month") || lowerText.contains("year") || lowerText.contains("all") || lowerText.contains("and") ||
                    lowerText.contains("with") || lowerText.contains("for");

                // Check if this is actually a budget or category budget request
                boolean isBudgetRequest = BudgetMessageHelper.isBudgetQuery(text) || lowerText.contains("danh mục") || lowerText.contains("category");

                if (hasBulkIndicators && !isBudgetRequest) {
                    isExpenseBulkMode = true;
                    android.util.Log.d("RequestRouterUseCase", "Detected expense bulk request from text content: " + text);
                }
            }
        }

        // Normal send to AI for expense tracking
        promptUseCase.sendPromptToAI(text, activity, messages, chatAdapter, messagesRecycler, textToSpeech, updateNetworkStatusCallback);
    }
}