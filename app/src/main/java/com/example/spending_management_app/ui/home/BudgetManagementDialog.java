package com.example.spending_management_app.ui.home;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.spending_management_app.R;
import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.BudgetEntity;
import com.example.spending_management_app.ui.AiChatBottomSheet;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.io.IOException;
import java.io.IOException;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import org.json.JSONArray;
import org.json.JSONObject;

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

    btnAddIncome.setOnClickListener(v -> {
            handleMonthlyBudget();
            dismiss(); // Close dialog
        });

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
        // Open AI chat bottom sheet with "budget" mode flag
        AiChatBottomSheet aiChatBottomSheet = new AiChatBottomSheet();
        Bundle args = new Bundle();
        args.putString("mode", "budget_management"); // New flag to indicate budget mode
        aiChatBottomSheet.setArguments(args);
        aiChatBottomSheet.show(getParentFragmentManager(), aiChatBottomSheet.getTag());
    }
}