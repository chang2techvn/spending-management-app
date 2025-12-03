package com.example.spending_management_app.domain.usecase.expense;

import android.app.Activity;

import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.R;
import com.example.spending_management_app.data.local.entity.TransactionEntity;
import com.example.spending_management_app.domain.repository.ExpenseRepository;
import com.example.spending_management_app.presentation.dialog.AiChatBottomSheet;
import com.example.spending_management_app.utils.FragmentRefreshHelper;
import com.example.spending_management_app.utils.ToastHelper;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

/**
 * Service class for handling expense operations
 */
public class ExpenseUseCase {

    private final ExpenseRepository expenseRepository;

    public ExpenseUseCase(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    /**
     * Save expense directly from AI response JSON
     */
    public void saveExpenseDirectly(String jsonString, Activity activity,
                                         List<AiChatBottomSheet.ChatMessage> messages,
                                         AiChatBottomSheet.ChatAdapter chatAdapter,
                                         RecyclerView messagesRecycler,
                                         Runnable refreshExpenseWelcomeMessageCallback) {
        android.util.Log.d("ExpenseService", "saveExpenseDirectly called with: " + jsonString);

        try {
            // Parse JSON từ AI response
            JSONObject json = new JSONObject(jsonString);
            android.util.Log.d("ExpenseService", "JSON parsed successfully");

            if (json != null) {
                // Lấy giá trị từ JSON
                String name = json.optString("name", "");
                double amount = json.optDouble("amount", 0.0);
                String category = json.optString("category", "");
                String currency = json.optString("currency", "VND");
                String type = json.optString("type", "expense");
                int day = json.optInt("day", Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
                int month = json.optInt("month", Calendar.getInstance().get(Calendar.MONTH) + 1);
                int year = json.optInt("year", Calendar.getInstance().get(Calendar.YEAR));

                android.util.Log.d("ExpenseService", "Extracted data: name=" + name + ", amount=" + amount + ", category=" + category);

                // Tạo Calendar object
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month - 1, day); // Month is 0-based

                // Tạo TransactionEntity với constructor đúng
                long transactionAmount = type.equals("expense") ? -Math.abs((long)amount) : (long)amount;
                TransactionEntity transaction = new TransactionEntity(
                    name,                    // description
                    category,                // category
                    transactionAmount,       // amount (negative for expense)
                    calendar.getTime(),      // date
                    type                     // type
                );

                android.util.Log.d("ExpenseService", "Transaction entity created, starting save process");

                // Lưu vào database trong background thread
                new Thread(() -> {
                    android.util.Log.d("ExpenseService", "Background thread started for database save");
                    try {
                        expenseRepository.insert(transaction);
                        android.util.Log.d("ExpenseService", "Database save successful");

                        // Hiển thị toast trên main thread với layer cao nhất
                        activity.runOnUiThread(() -> {
                            android.util.Log.d("ExpenseService", "Back on UI thread, preparing toast");
                            String transactionType = type.equals("expense") ? 
                                activity.getString(R.string.expense_type) :
                                activity.getString(R.string.income_type);
                            
                            String toastMessage = String.format(activity.getString(R.string.expense_added_toast),
                                transactionType, amount, currency, category);

                            android.util.Log.d("ExpenseService", "Toast message: " + toastMessage);

                            // Hiển thị 1 toast duy nhất ở TOP với UI đẹp
                            ToastHelper.showToastOnTop(activity, toastMessage);

                            // Refresh HomeFragment if available
                            refreshHomeFragment(activity);

                            // Also refresh HistoryFragment if it exists
                            refreshHistoryFragment(activity);

                            // Refresh expense welcome message
                            if (refreshExpenseWelcomeMessageCallback != null) {
                                refreshExpenseWelcomeMessageCallback.run();
                            }
                        });

                        // Hiển thị message trong chat trên main thread
                        activity.runOnUiThread(() -> {
                            // Chỉ hiển thị toast, không thêm message nữa vì AI đã trả về display text rồi
                            android.util.Log.d("ExpenseService", "Skipping additional chat message - AI already provided response");
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        activity.runOnUiThread(() -> {
                            String errorMessage = String.format(activity.getString(R.string.save_data_error), e.getMessage());
                            ToastHelper.showErrorToast(activity, errorMessage);
                            android.util.Log.e("ExpenseService", "Error saving expense", e);
                        });
                    }
                }).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = String.format(activity.getString(R.string.process_data_error), e.getMessage());
            ToastHelper.showErrorToast(activity, errorMessage);
            android.util.Log.e("ExpenseService", "Error processing data", e);
        }
    }

    /**
     * Refresh HomeFragment - delegates to FragmentRefreshHelper
     */
    private static void refreshHomeFragment(Activity activity) {
        FragmentRefreshHelper.refreshHomeFragment(activity);
    }

    /**
     * Refresh HistoryFragment - delegates to FragmentRefreshHelper
     */
    private static void refreshHistoryFragment(Activity activity) {
        FragmentRefreshHelper.refreshHistoryFragment(activity);
    }
}