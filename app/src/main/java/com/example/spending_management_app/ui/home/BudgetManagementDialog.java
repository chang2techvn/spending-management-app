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

public class BudgetManagementDialog extends DialogFragment {

    private OnActionSelectedListener listener;

    public interface OnActionSelectedListener {
        void onAddIncomeSelected();
        void onSetBudgetSelected();
    }

    public void setOnActionSelectedListener(OnActionSelectedListener listener) {
        this.listener = listener;
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
            if (listener != null) {
                listener.onAddIncomeSelected();
            }
            dismiss();
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
}