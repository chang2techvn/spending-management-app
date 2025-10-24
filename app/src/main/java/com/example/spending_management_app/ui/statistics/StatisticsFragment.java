package com.example.spending_management_app.ui.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.spending_management_app.databinding.FragmentStatisticsBinding;

public class StatisticsFragment extends Fragment {

    private FragmentStatisticsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        StatisticsViewModel statisticsViewModel =
                new ViewModelProvider(this).get(StatisticsViewModel.class);

        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup statistics data
        setupStatisticsData();

        return root;
    }

    private void setupStatisticsData() {
        // Sample data - in real app, get from database
        long totalIncome = 8000000;
        long totalExpense = 5500000;
        long balance = totalIncome - Math.abs(totalExpense);

        binding.totalIncome.setText(formatCurrency(totalIncome));
        binding.totalExpense.setText(formatCurrency(Math.abs(totalExpense)));

        // Budget remaining - sample
        long budgetRemaining = 3000000;
        // Add budget remaining text if layout has it

        // Chart placeholder click listener
        binding.chartPlaceholder.setOnClickListener(v -> {
            // TODO: Open detailed chart view
        });
    }

    private String formatCurrency(long amount) {
        String amountStr = String.valueOf(Math.abs(amount));
        StringBuilder formatted = new StringBuilder();
        int count = 0;
        for (int i = amountStr.length() - 1; i >= 0; i--) {
            formatted.insert(0, amountStr.charAt(i));
            count++;
            if (count % 3 == 0 && i > 0) {
                formatted.insert(0, ",");
            }
        }
        return formatted.toString() + " VND";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
