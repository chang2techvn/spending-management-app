package com.example.spending_management_app.ui.home;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.spending_management_app.R;
import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.entity.BudgetEntity;
import com.example.spending_management_app.ui.AiChatBottomSheet;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class BudgetManagementDialog extends DialogFragment {

    private OnActionSelectedListener listener;
    private AppDatabase db;

    public interface OnActionSelectedListener {
        void onAddIncomeSelected();
        void onSetBudgetSelected();
    }

    public void setOnActionSelectedListener(OnActionSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(getContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getContext(), R.style.RoundedDialog4Corners);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_budget_management, null);
        dialog.setContentView(view);

        Button btnAddIncome = view.findViewById(R.id.btn_add_income);
        Button btnSetBudget = view.findViewById(R.id.btn_set_budget);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

    btnAddIncome.setOnClickListener(v -> handleMonthlyBudget());

        btnSetBudget.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSetBudgetSelected();
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        return dialog;
    }

    private void handleMonthlyBudget() {
        // Query DB on background thread, then open AI chat with context prompt
        Executors.newSingleThreadExecutor().execute(() -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date startOfMonth = cal.getTime();
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            Date endOfMonth = cal.getTime();

            List<BudgetEntity> monthlyBudgets = db.budgetDao().getBudgetsByDateRange(startOfMonth, endOfMonth);

            getActivity().runOnUiThread(() -> {
                String prompt;
                if (monthlyBudgets == null || monthlyBudgets.isEmpty()) {
                    // No budget set: ask AI to prompt user naturally and request an amount
                    prompt = "Người dùng chưa thiết lập ngân sách cho tháng này. Hãy hỏi họ bằng một câu tự nhiên, đa dạng (không quá cứng nhắc) để yêu cầu họ nhập số ngân sách cho tháng này. Sau câu hỏi, khi người dùng trả lời, hãy trả về JSON dạng {\"action\":\"set_budget\", \"amount\": số, \"currency\": \"VND\"} để app có thể lưu." 
                            + " Hãy đưa ra một câu hỏi kèm theo gợi ý ngắn nếu cần.";
                } else {
                    BudgetEntity budget = monthlyBudgets.get(0);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
                    String dateStr = budget.getDate() != null ? dateFormat.format(budget.getDate()) : "(không xác định)";
                    prompt = "Người dùng đã thiết lập ngân sách tháng này vào ngày " + dateStr + ". Số tiền hiện tại là "
                            + String.format(Locale.getDefault(), "%,d", budget.getMonthlyLimit()) + " VND. Hãy trả lời người dùng bằng ngôn ngữ tự nhiên (có thể hài hước), thông báo số ngân sách hiện tại và hỏi xem họ có muốn thay đổi không. Nếu user muốn thay đổi và cung cấp số mới, trả về JSON {\"action\":\"update_budget\", \"amount\": số, \"currency\": \"VND\"}.";
                }

                // Open AI chat bottom sheet with the crafted prompt
                AiChatBottomSheet aiChatBottomSheet = new AiChatBottomSheet();
                Bundle args = new Bundle();
                args.putString("initial_prompt", prompt);
                aiChatBottomSheet.setArguments(args);
                aiChatBottomSheet.show(getChildFragmentManager(), aiChatBottomSheet.getTag());
            });
        });
    }
}