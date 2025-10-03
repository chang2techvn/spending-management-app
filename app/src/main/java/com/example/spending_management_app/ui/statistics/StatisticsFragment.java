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
        // Set total expense and income
        binding.totalExpense.setText("5,500,000 VND");
        binding.totalIncome.setText("8,000,000 VND");

        // Chart placeholder click listener (for future chart implementation)
        binding.chartPlaceholder.setOnClickListener(v -> {
            // TODO: Open detailed chart view or implement chart library
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
