package com.example.spending_management_app.ui.history;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.spending_management_app.databinding.DialogDateRangePickerBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateRangePickerDialog extends BottomSheetDialogFragment {

    private DialogDateRangePickerBinding binding;
    private DateRangeListener listener;
    private Date startDate;
    private Date endDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));

    public interface DateRangeListener {
        void onDateRangeSelected(Date startDate, Date endDate);
    }

    public void setDateRangeListener(DateRangeListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogDateRangePickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupQuickDateButtons();
        setupCustomDateButtons();
        setupActionButtons();
    }

    private void setupQuickDateButtons() {
        binding.btnToday.setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            startDate = today.getTime();
            endDate = today.getTime();
            updateDateButtons();
        });

        binding.btnThisWeek.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            startDate = calendar.getTime();

            calendar.add(Calendar.DAY_OF_WEEK, 6);
            endDate = calendar.getTime();
            updateDateButtons();
        });

        binding.btnThisMonth.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            startDate = calendar.getTime();

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            endDate = calendar.getTime();
            updateDateButtons();
        });
    }

    private void setupCustomDateButtons() {
        binding.btnStartDate.setOnClickListener(v -> showDatePicker(true));
        binding.btnEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void setupActionButtons() {
        binding.btnCancel.setOnClickListener(v -> dismiss());

        binding.btnApply.setOnClickListener(v -> {
            if (listener != null && startDate != null && endDate != null) {
                listener.onDateRangeSelected(startDate, endDate);
            }
            dismiss();
        });
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        if (isStartDate && startDate != null) {
            calendar.setTime(startDate);
        } else if (!isStartDate && endDate != null) {
            calendar.setTime(endDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (DatePicker view, int year, int month, int dayOfMonth) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, dayOfMonth);

                if (isStartDate) {
                    startDate = selectedCalendar.getTime();
                } else {
                    endDate = selectedCalendar.getTime();
                }
                updateDateButtons();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void updateDateButtons() {
        if (startDate != null) {
            binding.btnStartDate.setText(dateFormat.format(startDate));
        }
        if (endDate != null) {
            binding.btnEndDate.setText(dateFormat.format(endDate));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}