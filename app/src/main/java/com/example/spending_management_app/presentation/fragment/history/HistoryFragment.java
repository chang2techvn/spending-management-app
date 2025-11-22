package com.example.spending_management_app.presentation.fragment.history;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.spending_management_app.data.local.entity.BudgetHistoryEntity;
import com.example.spending_management_app.databinding.FragmentHistoryBinding;
import com.example.spending_management_app.domain.model.Transaction;
import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.local.entity.TransactionEntity;
import com.example.spending_management_app.presentation.viewmodel.history.HistoryViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;

public class HistoryFragment extends Fragment implements DateRangePickerDialog.DateRangeListener {

    private FragmentHistoryBinding binding;
    private SectionedTransactionAdapter transactionAdapter;
    private List<Transaction> allTransactions;
    private List<Transaction> filteredTransactions;
    private String currentQuery = "";
    private Date startDateFilter;
    private Date endDateFilter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HistoryViewModel historyViewModel =
                new ViewModelProvider(this).get(HistoryViewModel.class);

        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup transaction data from database
        loadTransactionsFromDatabase();

        // Setup filter tabs
        setupFilterTabs();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup search functionality
        setupSearch();

        // Setup date filter
        setupDateFilter();

        return root;
    }

    private void loadTransactionsFromDatabase() {
        // Initialize empty lists first
        allTransactions = new ArrayList<>();
        filteredTransactions = new ArrayList<>();
        
        // Load data from database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get all transactions from database
                List<TransactionEntity> transactionEntities = AppDatabase.getInstance(getContext())
                        .transactionDao()
                        .getAllTransactions();
                
                // Get all budget history from database
                List<BudgetHistoryEntity> budgetHistoryEntities =
                        AppDatabase.getInstance(getContext())
                                .budgetHistoryDao()
                                .getAllBudgetHistory();
                
                // Convert TransactionEntity to Transaction objects
                List<Transaction> transactions = new ArrayList<>();
                for (TransactionEntity entity : transactionEntities) {
                    // Choose appropriate icon based on category and type
                    String iconName = getIconForCategory(entity.category, entity.type);
                    
                    Transaction transaction = new Transaction(
                            entity.description,
                            entity.category,
                            entity.amount,
                            iconName,
                            entity.date,
                            entity.type
                    );
                    transactions.add(transaction);
                }
                
                // Convert BudgetHistoryEntity to Transaction objects
                for (BudgetHistoryEntity entity : budgetHistoryEntities) {
                    String category = entity.getBudgetType().equals("monthly") ? "Ngân sách tháng" : entity.getCategory();
                    String iconName = "ic_account_balance_wallet";
                    
                    // Determine amount sign based on action
                    long displayAmount;
                    if ("delete".equals(entity.getAction())) {
                        // Delete action: show negative amount (red color)
                        displayAmount = -Math.abs(entity.getAmount());
                    } else if ("update".equals(entity.getAction())) {
                        // Update action: amount is already delta (can be positive or negative)
                        // Positive = increase (green), Negative = decrease (red)
                        displayAmount = entity.getAmount();
                    } else {
                        // Create action: show positive amount (green color)
                        displayAmount = Math.abs(entity.getAmount());
                    }
                    
                    Transaction transaction = new Transaction(
                            entity.getDescription(),
                            category,
                            displayAmount,
                            iconName,
                            entity.getDate(),
                            "budget" // New type for budget history
                    );
                    transactions.add(transaction);
                }
                
                // Sort all transactions by date (newest first)
                transactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allTransactions.clear();
                        allTransactions.addAll(transactions);
                        
                        filteredTransactions.clear();
                        filteredTransactions.addAll(allTransactions);
                        
                        // Refresh adapter
                        if (transactionAdapter != null) {
                            transactionAdapter.updateTransactions(filteredTransactions);
                        }
                        updateEmptyState();
                        
                        android.util.Log.d("HistoryFragment", "Loaded " + transactions.size() + " items (transactions + budget history) from database");
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("HistoryFragment", "Error loading data from database", e);
                
                // Fallback to sample data on error
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        setupSampleTransactionData();
                    });
                }
            }
        });
    }

    private void setupSampleTransactionData() {
        // Fallback sample data (keep original method name for compatibility)
        // Create sample transaction data with various dates
        allTransactions = new ArrayList<>();

        // Today's transactions
        Date today = new Date();
        allTransactions.add(new Transaction("Lương tháng 10", "Ngân sách", 8000000, "ic_home_black_24dp", today, "income"));
        allTransactions.add(new Transaction("Ăn trưa tại quán cơm", "Ăn uống", -45000, "ic_bar_chart", today, "expense"));
        allTransactions.add(new Transaction("Tiền xăng đi làm", "Di chuyển", -120000, "ic_bar_chart", today, "expense"));

        // Yesterday's transactions
        Date yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);
        allTransactions.add(new Transaction("Mua áo mới", "Mua sắm", -350000, "ic_bar_chart", yesterday, "expense"));
        allTransactions.add(new Transaction("Cà phê sáng", "Ăn uống", -25000, "ic_bar_chart", yesterday, "expense"));

        // 2 days ago
        Date twoDaysAgo = new Date(today.getTime() - 2 * 24 * 60 * 60 * 1000);
        allTransactions.add(new Transaction("Tiền điện tháng 10", "Tiện ích", -850000, "ic_bar_chart", twoDaysAgo, "expense"));
        allTransactions.add(new Transaction("Bán đồ cũ", "Ngân sách", 500000, "ic_home_black_24dp", twoDaysAgo, "income"));

        // 3 days ago
        Date threeDaysAgo = new Date(today.getTime() - 3 * 24 * 60 * 60 * 1000);
        allTransactions.add(new Transaction("Taxi về quê", "Di chuyển", -200000, "ic_bar_chart", threeDaysAgo, "expense"));
        allTransactions.add(new Transaction("Mua sách", "Giáo dục", -150000, "ic_bar_chart", threeDaysAgo, "expense"));
        allTransactions.add(new Transaction("Ăn tối gia đình", "Ăn uống", -180000, "ic_bar_chart", threeDaysAgo, "expense"));

        filteredTransactions = new ArrayList<>(allTransactions);
        
        android.util.Log.d("HistoryFragment", "Sample data loaded with " + allTransactions.size() + " transactions");
    }

    private void setupFilterTabs() {
        // Add tabs for filtering
        binding.transactionFilterTabs.addTab(binding.transactionFilterTabs.newTab().setText("Tất cả"));
        binding.transactionFilterTabs.addTab(binding.transactionFilterTabs.newTab().setText("Ngân sách"));
        binding.transactionFilterTabs.addTab(binding.transactionFilterTabs.newTab().setText("Chi tiêu"));

        binding.transactionFilterTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterTransactions(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        transactionAdapter = new SectionedTransactionAdapter(filteredTransactions);
        binding.transactionsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.transactionsRecycler.setAdapter(transactionAdapter);

        updateEmptyState();
    }

    private void setupDateFilter() {
        binding.dateFilterButton.setOnClickListener(v -> {
            DateRangePickerDialog dialog = new DateRangePickerDialog();
            dialog.setDateRangeListener(this);
            dialog.show(getChildFragmentManager(), "dateRangePicker");
        });
    }

    private void filterTransactions(int tabPosition) {
        applyFiltersAndSearch();
    }

    private void setupSearch() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        currentQuery = query;
        applyFiltersAndSearch();
    }

    private void applyFiltersAndSearch() {
        List<Transaction> searchResults = new ArrayList<>();

        // Apply date filter first
        List<Transaction> dateFiltered = new ArrayList<>();
        if (startDateFilter != null && endDateFilter != null) {
            // Normalize start date to beginning of day (00:00:00)
            java.util.Calendar startCal = java.util.Calendar.getInstance();
            startCal.setTime(startDateFilter);
            startCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            startCal.set(java.util.Calendar.MINUTE, 0);
            startCal.set(java.util.Calendar.SECOND, 0);
            startCal.set(java.util.Calendar.MILLISECOND, 0);
            Date normalizedStartDate = startCal.getTime();
            
            // Normalize end date to end of day (23:59:59)
            java.util.Calendar endCal = java.util.Calendar.getInstance();
            endCal.setTime(endDateFilter);
            endCal.set(java.util.Calendar.HOUR_OF_DAY, 23);
            endCal.set(java.util.Calendar.MINUTE, 59);
            endCal.set(java.util.Calendar.SECOND, 59);
            endCal.set(java.util.Calendar.MILLISECOND, 999);
            Date normalizedEndDate = endCal.getTime();
            
            for (Transaction transaction : allTransactions) {
                if (transaction.getDate().compareTo(normalizedStartDate) >= 0 &&
                    transaction.getDate().compareTo(normalizedEndDate) <= 0) {
                    dateFiltered.add(transaction);
                }
            }
        } else {
            dateFiltered.addAll(allTransactions);
        }

        // Apply search filter
        if (currentQuery.isEmpty()) {
            searchResults.addAll(dateFiltered);
        } else {
            for (Transaction transaction : dateFiltered) {
                if (transaction.getDescription().toLowerCase().contains(currentQuery.toLowerCase()) ||
                    transaction.getCategory().toLowerCase().contains(currentQuery.toLowerCase())) {
                    searchResults.add(transaction);
                }
            }
        }

        // Apply current filter to search results
        int currentTab = binding.transactionFilterTabs.getSelectedTabPosition();
        filteredTransactions.clear();

        switch (currentTab) {
            case 0: // Tất cả
                filteredTransactions.addAll(searchResults);
                break;
            case 1: // Ngân sách
                filteredTransactions.addAll(searchResults.stream()
                    .filter(t -> "budget".equals(t.getType()))
                    .collect(Collectors.toList()));
                break;
            case 2: // Chi tiêu
                filteredTransactions.addAll(searchResults.stream()
                    .filter(t -> "expense".equals(t.getType()))
                    .collect(Collectors.toList()));
                break;
        }

        transactionAdapter.updateTransactions(filteredTransactions);
        updateEmptyState();
    }

    @Override
    public void onDateRangeSelected(Date startDate, Date endDate) {
        startDateFilter = startDate;
        endDateFilter = endDate;
        applyFiltersAndSearch();

        // Update button appearance to show filter is active
        binding.dateFilterButton.setColorFilter(getResources().getColor(android.R.color.holo_blue_dark));
    }

    private void updateEmptyState() {
        if (filteredTransactions.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.transactionsRecycler.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.transactionsRecycler.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    // Method to refresh transactions when new ones are added
    public void refreshTransactions() {
        if (isAdded() && getContext() != null) {
            loadTransactionsFromDatabase();
        }
    }
    
    // Method to be called when the fragment becomes visible
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadTransactionsFromDatabase();
    }
    
    // Helper method to get appropriate icon for category
    private String getIconForCategory(String category, String type) {
        if ("income".equals(type)) {
            return "ic_home_black_24dp";
        }
        
        switch (category) {
            // Nhu cầu thiết yếu
            case "Ăn uống":
                return "🍽️";
            case "Di chuyển":
                return "🚗";
            case "Tiện ích":
                return "⚡";
            case "Y tế":
                return "🏥";
            case "Nhà ở":
                return "🏠";
            
            // Mua sắm & Phát triển bản thân
            case "Mua sắm":
                return "🛍️";
            case "Giáo dục":
                return "📚";
            case "Sách & Học tập":
                return "📖";
            case "Thể thao":
                return "⚽";
            case "Sức khỏe & Làm đẹp":
                return "💆";
            
            // Giải trí & Xã hội
            case "Giải trí":
                return "🎬";
            case "Du lịch":
                return "✈️";
            case "Ăn ngoài & Cafe":
                return "☕";
            case "Quà tặng & Từ thiện":
                return "🎁";
            case "Hội họp & Tiệc tụng":
                return "🎉";
            
            // Công nghệ & Dịch vụ
            case "Điện thoại & Internet":
                return "📱";
            case "Đăng ký & Dịch vụ":
                return "💳";
            case "Phần mềm & Apps":
                return "💻";
            case "Ngân hàng & Phí":
                return "🏦";
            
            // Gia đình & Con cái
            case "Con cái":
                return "👶";
            case "Thú cưng":
                return "🐕";
            case "Gia đình":
                return "👨‍👩‍👧‍👦";
            
            // Thu nhập & Tài chính
            case "Lương":
                return "💰";
            case "Đầu tư":
                return "📈";
            case "Thu nhập phụ":
                return "💵";
            case "Tiết kiệm":
                return "🏦";
            
            // Khác
            case "Khác":
                return "�";
            default:
                return "💳";
        }
    }
}
