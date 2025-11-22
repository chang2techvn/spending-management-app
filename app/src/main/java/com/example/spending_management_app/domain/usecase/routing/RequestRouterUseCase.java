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
import com.example.spending_management_app.domain.usecase.budget.BudgetMessageHelper;
import com.example.spending_management_app.domain.usecase.category.CategoryBudgetUseCase;
import com.example.spending_management_app.domain.usecase.expense.ExpenseMessageHelper;
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

        // If offline, try to handle with regex first
        if (!isOnline) {
            boolean handled = callback.handleOfflineRequest(text, isBudgetMode, isCategoryBudgetMode, isExpenseBulkMode);
            if (handled) {
                return;
            }
            // If not handled by regex, show error
            messages.add(new ChatMessage("❌ Chức năng này cần kết nối internet. Vui lòng kiểm tra kết nối mạng của bạn.", false, "Bây giờ"));
            chatAdapter.notifyItemInserted(messages.size() - 1);
            messagesRecycler.smoothScrollToPosition(messages.size() - 1);
            return;
        }

        // Handle expense bulk management
        if (isExpenseBulkMode) {
            callback.handleExpenseBulkRequest(text);
            return;
        }

        // Handle category budget management
        if (isCategoryBudgetMode) {
            categoryBudgetUseCase.handleCategoryBudgetRequest(text, context, activity, messages, chatAdapter, messagesRecycler, callback::refreshHomeFragment, callback::refreshCategoryBudgetWelcomeMessage);
            return;
        }

        // Check if user is asking for budget analysis, view, or delete
        if (isBudgetMode || BudgetMessageHelper.isBudgetQuery(text)) {
            callback.handleBudgetQuery(text);
            return;
        }

        // Check if user is asking for financial analysis or reports
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

        // Normal send to AI for expense tracking
        promptUseCase.sendPromptToAI(text, activity, messages, chatAdapter, messagesRecycler, textToSpeech, updateNetworkStatusCallback);
    }
}