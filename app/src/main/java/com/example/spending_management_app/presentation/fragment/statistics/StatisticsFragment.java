package com.example.spending_management_app.presentation.fragment.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.spending_management_app.R;
import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.local.dao.BudgetDao;
import com.example.spending_management_app.data.local.entity.MonthlySpending;
import com.example.spending_management_app.data.local.dao.TransactionDao;
import com.example.spending_management_app.data.local.entity.TransactionEntity;
import com.example.spending_management_app.databinding.FragmentStatisticsBinding;
import com.example.spending_management_app.presentation.viewmodel.statistics.StatisticsViewModel;
import com.example.spending_management_app.utils.CategoryUtils;
import com.example.spending_management_app.utils.CurrencyFormatter;
import com.example.spending_management_app.utils.UserSession;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import android.widget.PopupMenu;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class StatisticsFragment extends Fragment {

    private FragmentStatisticsBinding binding;
    private TransactionDao transactionDao;
    private BudgetDao budgetDao;
    private String selectedYear;
    private Observer<List<MonthlySpending>> currentObserver;
    private UserSession userSession;

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
        
        // Initialize UserSession
        userSession = UserSession.getInstance(requireContext());

        // Setup year spinner
        setupYearSpinner();

        // Setup statistics data
        setupStatisticsData();
        
        // Setup download report button
        setupDownloadReportButton();
        
        // Setup monthly spending chart (will be called after year is selected)
        // setupMonthlySpendingChart() is now called from year spinner
        
        // Setup category spending (will be updated when year changes)
        loadCategorySpendingForYear(selectedYear);

        // Load year statistics (will be updated when year changes)
        loadYearStatistics(selectedYear);

        return root;
    }

    private void setupYearSpinner() {
        // Get current year as default
        selectedYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        
        // Set initial year text
        binding.yearText.setText(selectedYear);
        
        // Load available years from database in background
        Executors.newSingleThreadExecutor().execute(() -> {
            int userId = userSession.getCurrentUserId();
            List<String> years = transactionDao.getDistinctYears(userId);
            
            // If no data, add current year
            if (years == null || years.isEmpty()) {
                years = new ArrayList<>();
                years.add(selectedYear);
            }
            
            List<String> finalYears = years;
            
            // Update UI on main thread
            requireActivity().runOnUiThread(() -> {
                // Setup click listener for dropdown
                binding.yearDropdownContainer.setOnClickListener(v -> {
                    // Change icon to arrow up when opening dropdown
                    binding.dropdownIcon.setImageResource(com.example.spending_management_app.R.drawable.ic_arrow_drop_up);
                    
                    // Create popup menu
                    PopupMenu popupMenu = new PopupMenu(requireContext(), binding.yearDropdownContainer);
                    
                    // Add menu items for each year
                    for (int i = 0; i < finalYears.size(); i++) {
                        popupMenu.getMenu().add(0, i, i, finalYears.get(i));
                    }
                    
                    // Set menu item click listener
                    popupMenu.setOnMenuItemClickListener(item -> {
                        int position = item.getItemId();
                        selectedYear = finalYears.get(position);
                        
                        // Update year text
                        binding.yearText.setText(selectedYear);
                        
                        // Reload chart with selected year
                        setupMonthlySpendingChart(selectedYear);
                        
                        // Reload category spending with selected year
                        loadCategorySpendingForYear(selectedYear);
                        
                        // Reload year statistics with selected year
                        loadYearStatistics(selectedYear);
                        
                        return true;
                    });
                    
                    // Change icon back to arrow down when menu is dismissed
                    popupMenu.setOnDismissListener(menu -> {
                        binding.dropdownIcon.setImageResource(com.example.spending_management_app.R.drawable.ic_arrow_drop_down);
                    });
                    
                    // Show popup menu
                    popupMenu.show();
                });
                
                // Initial chart load with current year
                setupMonthlySpendingChart(selectedYear);
            });
        });
    }

    private void setupStatisticsData() {
        // Will be loaded by year in loadYearStatistics()
    }
    
    private void loadYearStatistics(String year) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Calculate year range
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, Integer.parseInt(year));
                cal.set(Calendar.MONTH, Calendar.JANUARY);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                java.util.Date startOfYear = cal.getTime();
                
                cal.set(Calendar.MONTH, Calendar.DECEMBER);
                cal.set(Calendar.DAY_OF_MONTH, 31);
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                java.util.Date endOfYear = cal.getTime();
                
                // Get total budget for the year
                int userId = userSession.getCurrentUserId();
                Long totalBudgetLong = budgetDao.getTotalBudgetByDateRange(userId, startOfYear, endOfYear);
                long totalBudget = (totalBudgetLong != null) ? totalBudgetLong : 0;
                
                // Get total expense for the year
                Long totalExpenseLong = transactionDao.getTotalExpenseByDateRange(userId, startOfYear, endOfYear);
                long totalExpense = (totalExpenseLong != null) ? Math.abs(totalExpenseLong) : 0;
                
                android.util.Log.d("StatisticsFragment", "Year " + year + " - Budget: " + totalBudget + ", Expense: " + totalExpense);
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        binding.totalIncome.setText(formatCurrency(totalBudget));
                        binding.totalExpense.setText(formatCurrency(totalExpense));
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("StatisticsFragment", "Error loading year statistics", e);
            }
        });
    }
    
    private void setupMonthlySpendingChart(String year) {
        // Remove previous observer if exists
        if (currentObserver != null) {
            int userId = userSession.getCurrentUserId();
            LiveData<List<MonthlySpending>> previousLiveData = 
                    transactionDao.getMonthlySpendingByYearLive(userId, selectedYear);
            previousLiveData.removeObserver(currentObserver);
        }
        
        // Create new observer
        currentObserver = monthlyData -> {
            LineChart chart = binding.monthlySpendingChart;
            
            if (monthlyData == null || monthlyData.isEmpty()) {
                // No data available
                chart.clear();
                chart.setNoDataText(getString(R.string.no_expense_data));
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
        int userId = userSession.getCurrentUserId();
        transactionDao.getMonthlySpendingByYearLive(userId, year).observe(getViewLifecycleOwner(), currentObserver);
    }

    private String formatCurrency(long amount) {
        return CurrencyFormatter.formatCurrency(getContext(), amount);
    }
    
    private String formatCurrencyShort(long amount) {
        return CurrencyFormatter.formatCurrencyShort(getContext(), amount);
    }
    
    private void loadCategorySpendingForYear(String year) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Calculate year range
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, Integer.parseInt(year));
                cal.set(Calendar.MONTH, Calendar.JANUARY);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                java.util.Date startOfYear = cal.getTime();
                
                cal.set(Calendar.MONTH, Calendar.DECEMBER);
                cal.set(Calendar.DAY_OF_MONTH, 31);
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                java.util.Date endOfYear = cal.getTime();
                
                // Get all transactions for this year by category
                int userId = userSession.getCurrentUserId();
                List<TransactionEntity> allTransactions =
                        transactionDao.getTransactionsByDateRange(userId, startOfYear, endOfYear);
                
                // Calculate spending by category
                java.util.Map<String, Long> categorySpending = new java.util.HashMap<>();
                long totalYearSpending = 0;
                
                for (TransactionEntity transaction : allTransactions) {
                    if ("expense".equals(transaction.type)) {
                        long amount = Math.abs(transaction.amount);
                        categorySpending.put(transaction.category, 
                                categorySpending.getOrDefault(transaction.category, 0L) + amount);
                        totalYearSpending += amount;
                    }
                }
                
                // Create list of category data (using public fields to avoid reflection issues)
                class CategoryData {
                    public String category;
                    public long spending;
                    
                    CategoryData(String category, long spending) {
                        this.category = category;
                        this.spending = spending;
                    }
                }
                
                List<CategoryData> categoryDataList = new ArrayList<>();
                for (String category : categorySpending.keySet()) {
                    long spending = categorySpending.get(category);
                    categoryDataList.add(new CategoryData(category, spending));
                }
                
                // Sort by spending (highest to lowest)
                categoryDataList.sort((a, b) -> Long.compare(b.spending, a.spending));
                
                android.util.Log.d("StatisticsFragment", "Year " + year + " - Total categories: " + categoryDataList.size());
                android.util.Log.d("StatisticsFragment", "Total year spending: " + totalYearSpending);
                
                long finalTotalYearSpending = totalYearSpending;
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateCategorySpendingUI(categoryDataList, finalTotalYearSpending);
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("StatisticsFragment", "Error loading category spending for year", e);
            }
        });
    }
    
    private void updateCategorySpendingUI(List<?> categories, long totalSpending) {
        android.util.Log.d("StatisticsFragment", "updateCategorySpendingUI called with " + categories.size() + " categories");
        
        ViewGroup container = binding.categorySpendingContainer;
        
        if (container == null) {
            android.util.Log.e("StatisticsFragment", "categorySpendingContainer is NULL!");
            return;
        }
        
        // Remove all views except the first one (title)
        int childCount = container.getChildCount();
        if (childCount > 1) {
            container.removeViews(1, childCount - 1);
        }
        
        // If no spending data, show message
        if (categories.isEmpty() || totalSpending == 0) {
            android.widget.TextView noDataText = new android.widget.TextView(getContext());
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
            noDataText.setLayoutParams(params);
            noDataText.setText(getString(R.string.no_expense_data_this_year));
            noDataText.setTextColor(0xFF757575);
            noDataText.setTextSize(14);
            container.addView(noDataText);
            return;
        }
        
        // Add each category dynamically
        for (int i = 0; i < categories.size(); i++) {
            Object obj = categories.get(i);
            
            try {
                String category = (String) obj.getClass().getField("category").get(obj);
                long spending = (Long) obj.getClass().getField("spending").get(obj);
                
                android.util.Log.d("StatisticsFragment", "Adding category: " + category + 
                        ", spending=" + spending);
                
                // Create category view
                View categoryView = createCategorySpendingView(category, spending, totalSpending);
                container.addView(categoryView);
                
            } catch (Exception e) {
                android.util.Log.e("StatisticsFragment", "Error creating category view", e);
                e.printStackTrace();
            }
        }
        
        android.util.Log.d("StatisticsFragment", "Total views in container: " + container.getChildCount());
    }
    
    private View createCategorySpendingView(String category, long spending, long totalSpending) {
        // Create container
        android.widget.LinearLayout container = new android.widget.LinearLayout(getContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.LinearLayout.LayoutParams containerParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        container.setLayoutParams(containerParams);
        
        // Create header (name + percentage)
        android.widget.LinearLayout header = new android.widget.LinearLayout(getContext());
        header.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        android.widget.LinearLayout.LayoutParams headerParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerParams.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        header.setLayoutParams(headerParams);
        
        // Category name
        android.widget.TextView nameView = new android.widget.TextView(getContext());
        android.widget.LinearLayout.LayoutParams nameParams = new android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        nameView.setLayoutParams(nameParams);
        String localizedCategoryName = CategoryUtils.getLocalizedCategoryName(getContext(), category);
        String icon = getIconEmojiForCategory(category);
        nameView.setText(icon + " " + localizedCategoryName);
        nameView.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.text_primary));        nameView.setTextSize(14);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // Percentage with 1 decimal place for accuracy
        android.widget.TextView percentageView = new android.widget.TextView(getContext());
        percentageView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        double percentageDouble = (totalSpending > 0) ? (spending * 100.0) / totalSpending : 0;
        String percentageText;
        if (percentageDouble >= 0.1) {
            // Show 1 decimal place (e.g., "0.5%", "10.2%", "91.3%")
            percentageText = String.format(java.util.Locale.getDefault(), "%.1f%%", percentageDouble);
        } else if (percentageDouble > 0) {
            // Very small percentages, show 2 decimal places
            percentageText = String.format(java.util.Locale.getDefault(), "%.2f%%", percentageDouble);
        } else {
            percentageText = "0%";
        }
        percentageView.setText(percentageText);
        percentageView.setTextColor(0xFFF44336);
        percentageView.setTextSize(14);
        percentageView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        header.addView(nameView);
        header.addView(percentageView);
        
        // Progress bar (just visual, showing percentage)
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(
                getContext(), null, android.R.attr.progressBarStyleHorizontal);
        android.widget.LinearLayout.LayoutParams progressParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (8 * getResources().getDisplayMetrics().density)
        );
        progressParams.bottomMargin = (int) (4 * getResources().getDisplayMetrics().density);
        progressBar.setLayoutParams(progressParams);
        
        // Set rounded progress bar drawable
        progressBar.setProgressDrawable(getResources().getDrawable(com.example.spending_management_app.R.drawable.rounded_progress_bar, null));
        
        // Set progress (convert double to int for progress bar)
        int progressValue = (int)Math.round(percentageDouble);
        progressBar.setProgress(progressValue);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFF44336));
        progressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
        
        // Amount text (only spending, no budget)
        android.widget.TextView amountView = new android.widget.TextView(getContext());
        android.widget.LinearLayout.LayoutParams amountParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        amountParams.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        amountView.setLayoutParams(amountParams);
    amountView.setText(formatCurrency(spending));
        amountView.setTextColor(0xFF757575);
        amountView.setTextSize(12);
        
        // Add all views to container
        container.addView(header);
        container.addView(progressBar);
        container.addView(amountView);
        
        return container;
    }
    
    private String getIconEmojiForCategory(String category) {
        // Get the localized category name first, then get the icon
        String localizedCategory = CategoryUtils.getLocalizedCategoryName(getContext(), category);
        return CategoryUtils.getIconForCategory(localizedCategory);
    }
    
    private void setupDownloadReportButton() {
        binding.downloadReportButton.setOnClickListener(v -> {
            generateAndDownloadReport();
        });
    }
    
    private void generateAndDownloadReport() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int userId = userSession.getCurrentUserId();
                String reportContent = generateReportContent(userId, selectedYear);
                
                // Save to Downloads folder
                File downloadsDir = new File(requireContext().getExternalFilesDir(null), "Downloads");
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                
                String fileName = "BaoCaoThongKe_" + selectedYear + "_" + 
                    new SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date()) + ".txt";
                File reportFile = new File(downloadsDir, fileName);
                
                try (FileWriter writer = new FileWriter(reportFile)) {
                    writer.write(reportContent);
                }
                
                // Show success message on UI thread
                requireActivity().runOnUiThread(() -> {
                    android.widget.Toast.makeText(requireContext(), 
                        "✅ Báo cáo đã được tải về: " + reportFile.getAbsolutePath(), 
                        android.widget.Toast.LENGTH_LONG).show();
                });
                
            } catch (Exception e) {
                android.util.Log.e("StatisticsFragment", "Error generating report", e);
                requireActivity().runOnUiThread(() -> {
                    android.widget.Toast.makeText(requireContext(), 
                        "❌ Lỗi khi tạo báo cáo: " + e.getMessage(), 
                        android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private String generateReportContent(int userId, String year) {
        StringBuilder report = new StringBuilder();
        report.append("📊 BÁO CÁO THỐNG KÊ CHI TIÊU\n");
        report.append("================================\n\n");
        report.append("Năm: ").append(year).append("\n");
        report.append("Ngày tạo: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date())).append("\n\n");
        
        try {
            // Get year date range
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(year), 0, 1, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            java.util.Date startOfYear = cal.getTime();
            
            cal.set(Integer.parseInt(year), 11, 31, 23, 59, 59);
            cal.set(Calendar.MILLISECOND, 999);
            java.util.Date endOfYear = cal.getTime();
            
            // Total expense for the year
            Long totalExpense = transactionDao.getTotalExpenseByDateRange(userId, startOfYear, endOfYear);
            report.append("💰 TỔNG CHI TIÊU NĂM: ").append(formatCurrency(totalExpense != null ? totalExpense : 0)).append("\n\n");
            
            // Monthly spending breakdown
            report.append("📅 CHI TIÊU THEO THÁNG:\n");
            report.append("--------------------\n");
            
            // Calculate monthly spending manually
            for (int month = 1; month <= 12; month++) {
                cal.set(Integer.parseInt(year), month - 1, 1, 0, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                java.util.Date monthStart = cal.getTime();
                
                cal.set(Integer.parseInt(year), month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
                cal.set(Calendar.MILLISECOND, 999);
                java.util.Date monthEnd = cal.getTime();
                
                Long monthExpense = transactionDao.getTotalExpenseByDateRange(userId, monthStart, monthEnd);
                if (monthExpense != null && monthExpense > 0) {
                    report.append(String.format("Tháng %02d: %s\n", month, formatCurrency(monthExpense)));
                }
            }
            report.append("\n");
            
            // Category spending breakdown
            report.append("📂 CHI TIÊU THEO DANH MỤC:\n");
            report.append("------------------------\n");
            
            List<TransactionEntity> allTransactions = transactionDao.getTransactionsByDateRange(userId, startOfYear, endOfYear);
            java.util.Map<String, Long> categorySpending = new java.util.HashMap<>();
            long totalYearSpending = 0;
            
            for (TransactionEntity transaction : allTransactions) {
                if ("expense".equals(transaction.type)) {
                    long amount = Math.abs(transaction.amount);
                    categorySpending.put(transaction.category, 
                        categorySpending.getOrDefault(transaction.category, 0L) + amount);
                    totalYearSpending += amount;
                }
            }
            
            // Sort categories by spending amount
            List<java.util.Map.Entry<String, Long>> sortedCategories = new ArrayList<>(categorySpending.entrySet());
            sortedCategories.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
            
            for (java.util.Map.Entry<String, Long> entry : sortedCategories) {
                double percentage = totalYearSpending > 0 ? (entry.getValue() * 100.0 / totalYearSpending) : 0;
                report.append(String.format("%s %s: %s (%.1f%%)\n", 
                    getIconEmojiForCategory(entry.getKey()),
                    entry.getKey(), 
                    formatCurrency(entry.getValue()),
                    percentage));
            }
            
            if (categorySpending.isEmpty()) {
                report.append("Không có dữ liệu chi tiêu theo danh mục\n");
            }
            
            report.append("\n");
            report.append("================================\n");
            report.append("Báo cáo được tạo bởi ứng dụng Quản lý Chi tiêu\n");
            
        } catch (Exception e) {
            report.append("Lỗi khi tạo báo cáo: ").append(e.getMessage()).append("\n");
        }
        
        return report.toString();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
