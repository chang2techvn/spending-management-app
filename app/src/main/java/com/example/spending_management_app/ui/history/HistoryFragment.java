package com.example.spending_management_app.ui.history;

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

import com.example.spending_management_app.databinding.FragmentHistoryBinding;
import com.example.spending_management_app.ui.home.Transaction;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

        // Setup transaction data
        setupTransactionData();

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

    private void setupTransactionData() {
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
            for (Transaction transaction : allTransactions) {
                if (transaction.getDate().compareTo(startDateFilter) >= 0 &&
                    transaction.getDate().compareTo(endDateFilter) <= 0) {
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
                    .filter(t -> t.getAmount() > 0)
                    .collect(Collectors.toList()));
                break;
            case 2: // Chi tiêu
                filteredTransactions.addAll(searchResults.stream()
                    .filter(t -> t.getAmount() < 0)
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
}
