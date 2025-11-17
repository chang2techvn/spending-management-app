package com.example.spending_management_app.ui.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.BudgetDao;
import com.example.spending_management_app.database.MonthlySpending;
import com.example.spending_management_app.database.TransactionDao;
import com.example.spending_management_app.databinding.FragmentStatisticsBinding;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class StatisticsFragment extends Fragment {

    private FragmentStatisticsBinding binding;
    private TransactionDao transactionDao;
    private BudgetDao budgetDao;
    private String selectedYear;
    private Observer<List<MonthlySpending>> currentObserver;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        StatisticsViewModel statisticsViewModel =
                new ViewModelProvider(this).get(StatisticsViewModel.class);

        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize DAOs
        AppDatabase database = AppDatabase.getInstance(requireContext());
        transactionDao = database.transactionDao();
        budgetDao = database.budgetDao();

        // Setup year spinner
        setupYearSpinner();

        // Setup statistics data
        setupStatisticsData();
        
        // Setup monthly spending chart (will be called after year is selected)
        // setupMonthlySpendingChart() is now called from year spinner

        return root;
    }

    private void setupYearSpinner() {
        // Get current year as default
        selectedYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        
        // Load available years from database in background
        Executors.newSingleThreadExecutor().execute(() -> {
            List<String> years = transactionDao.getDistinctYears();
            
            // If no data, add current year
            if (years == null || years.isEmpty()) {
                years = new ArrayList<>();
                years.add(selectedYear);
            }
            
            List<String> finalYears = years;
            
            // Update UI on main thread
            requireActivity().runOnUiThread(() -> {
                // Create adapter for spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        com.example.spending_management_app.R.layout.spinner_item,
                        finalYears
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.yearSpinner.setAdapter(adapter);
                
                // Set current year as selected
                int currentYearIndex = finalYears.indexOf(selectedYear);
                if (currentYearIndex >= 0) {
                    binding.yearSpinner.setSelection(currentYearIndex);
                }
                
                // Set listener for year selection
                binding.yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selectedYear = finalYears.get(position);
                        // Reload chart with selected year
                        setupMonthlySpendingChart(selectedYear);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // Do nothing
                    }
                });
            });
        });
    }

    private void setupStatisticsData() {
        // Observe total budget LiveData (sum of all months)
        budgetDao.getTotalBudgetLive().observe(getViewLifecycleOwner(), totalBudget -> {
            if (totalBudget == null) totalBudget = 0L;
            binding.totalIncome.setText(formatCurrency(totalBudget));
        });
        
        // Observe total expense LiveData
        transactionDao.getTotalExpenseLive().observe(getViewLifecycleOwner(), totalExpense -> {
            if (totalExpense == null) totalExpense = 0L;
            binding.totalExpense.setText(formatCurrency(Math.abs(totalExpense)));
        });
    }
    
    private void setupMonthlySpendingChart(String year) {
        // Remove previous observer if exists
        if (currentObserver != null) {
            LiveData<List<MonthlySpending>> previousLiveData = 
                    transactionDao.getMonthlySpendingByYearLive(selectedYear);
            previousLiveData.removeObserver(currentObserver);
        }
        
        // Create new observer
        currentObserver = monthlyData -> {
            LineChart chart = binding.monthlySpendingChart;
            
            if (monthlyData == null || monthlyData.isEmpty()) {
                // No data available
                chart.clear();
                chart.setNoDataText("Chưa có dữ liệu chi tiêu");
                chart.invalidate();
                return;
            }
            
            // Prepare data for chart
            List<Entry> entries = new ArrayList<>();
            List<String> monthLabels = new ArrayList<>();
            
            for (int i = 0; i < monthlyData.size(); i++) {
                MonthlySpending data = monthlyData.get(i);
                entries.add(new Entry(i, data.getTotal()));
                
                // Format month label (e.g., "2024-11" -> "T11")
                String[] parts = data.getMonth().split("-");
                if (parts.length == 2) {
                    monthLabels.add("T" + parts[1]);
                } else {
                    monthLabels.add(data.getMonth());
                }
            }
            
            // Create dataset
            LineDataSet dataSet = new LineDataSet(entries, "Chi tiêu");
            dataSet.setColor(Color.parseColor("#F44336"));
            dataSet.setCircleColor(Color.parseColor("#F44336"));
            dataSet.setLineWidth(2.5f);
            dataSet.setCircleRadius(5f);
            dataSet.setDrawCircleHole(false);
            dataSet.setValueTextSize(10f);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(Color.parseColor("#FFE5E5"));
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setDrawValues(false);
            dataSet.setHighLightColor(Color.parseColor("#D32F2F")); // Highlight color when touched
            dataSet.setHighlightLineWidth(2f);
            
            // Set data to chart
            LineData lineData = new LineData(dataSet);
            chart.setData(lineData);
            
            // Set custom MarkerView to show values on touch
            MonthMarkerView markerView = new MonthMarkerView(requireContext(), 
                    com.example.spending_management_app.R.layout.marker_view, monthLabels);
            markerView.setChartView(chart);
            chart.setMarker(markerView);
            
            // Customize chart appearance
            chart.getDescription().setEnabled(false);
            chart.setDrawGridBackground(false);
            chart.setTouchEnabled(true);
            chart.setDragEnabled(true);
            chart.setScaleEnabled(false);
            chart.setPinchZoom(false);
            chart.setDrawBorders(false);
            chart.setHighlightPerTapEnabled(true); // Enable highlighting on tap
            chart.setHighlightPerDragEnabled(false);
            
            // X axis
            XAxis xAxis = chart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);
            xAxis.setLabelCount(monthLabels.size());
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int index = (int) value;
                    if (index >= 0 && index < monthLabels.size()) {
                        return monthLabels.get(index);
                    }
                    return "";
                }
            });
            
            // Left Y axis
            YAxis leftAxis = chart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
            leftAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    if (value >= 1000000) {
                        return String.format("%.1ftr", value / 1000000);
                    } else if (value >= 1000) {
                        return String.format("%.0fn", value / 1000);
                    }
                    return String.format("%.0f", value);
                }
            });
            
            // Right Y axis
            chart.getAxisRight().setEnabled(false);
            
            // Legend
            chart.getLegend().setEnabled(false);
            
            // Animate only on first load (check if chart has data already)
            if (chart.getData() == null || chart.getData().getDataSetCount() == 0) {
                chart.animateX(1000);
            }
            
            // Refresh chart
            chart.notifyDataSetChanged();
            chart.invalidate();
        };
        
        // Observe monthly spending LiveData for selected year
        transactionDao.getMonthlySpendingByYearLive(year).observe(getViewLifecycleOwner(), currentObserver);
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
