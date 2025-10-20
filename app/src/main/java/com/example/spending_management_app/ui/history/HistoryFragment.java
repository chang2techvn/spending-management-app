package com.example.spending_management_app.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.spending_management_app.databinding.FragmentHistoryBinding;
import com.example.spending_management_app.ui.home.Transaction;
import com.example.spending_management_app.ui.home.TransactionAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> allTransactions;
    private List<Transaction> filteredTransactions;

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

        return root;
    }

    private void setupTransactionData() {
        // Create sample transaction data with dates
        allTransactions = new ArrayList<>();
        allTransactions.add(new Transaction("Lương tháng 10", "Thu nhập", 8000000, "ic_home_black_24dp"));
        allTransactions.add(new Transaction("Ăn trưa tại quán cơm", "Ăn uống", -45000, "ic_bar_chart"));
        allTransactions.add(new Transaction("Tiền xăng đi làm", "Di chuyển", -120000, "ic_bar_chart"));
        allTransactions.add(new Transaction("Mua áo mới", "Mua sắm", -350000, "ic_bar_chart"));
        allTransactions.add(new Transaction("Cà phê sáng", "Ăn uống", -25000, "ic_bar_chart"));
        allTransactions.add(new Transaction("Tiền điện tháng 10", "Tiện ích", -850000, "ic_bar_chart"));
        allTransactions.add(new Transaction("Bán đồ cũ", "Thu nhập", 500000, "ic_home_black_24dp"));
        allTransactions.add(new Transaction("Taxi về quê", "Di chuyển", -200000, "ic_bar_chart"));
        allTransactions.add(new Transaction("Mua sách", "Giáo dục", -150000, "ic_bar_chart"));
        allTransactions.add(new Transaction("Ăn tối gia đình", "Ăn uống", -180000, "ic_bar_chart"));

        filteredTransactions = new ArrayList<>(allTransactions);
    }

    private void setupFilterTabs() {
        // Add tabs for filtering
        binding.transactionFilterTabs.addTab(binding.transactionFilterTabs.newTab().setText("Tất cả"));
        binding.transactionFilterTabs.addTab(binding.transactionFilterTabs.newTab().setText("Thu nhập"));
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
        transactionAdapter = new TransactionAdapter(filteredTransactions);
        binding.transactionsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.transactionsRecycler.setAdapter(transactionAdapter);

        updateEmptyState();
    }

    private void filterTransactions(int tabPosition) {
        filteredTransactions.clear();

        switch (tabPosition) {
            case 0: // Tất cả
                filteredTransactions.addAll(allTransactions);
                break;
            case 1: // Thu nhập
                filteredTransactions.addAll(allTransactions.stream()
                    .filter(t -> t.getAmount() > 0)
                    .collect(Collectors.toList()));
                break;
            case 2: // Chi tiêu
                filteredTransactions.addAll(allTransactions.stream()
                    .filter(t -> t.getAmount() < 0)
                    .collect(Collectors.toList()));
                break;
        }

        transactionAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredTransactions.isEmpty()) {
            binding.transactionsRecycler.setVisibility(View.GONE);
            binding.emptyState.setVisibility(View.VISIBLE);
        } else {
            binding.transactionsRecycler.setVisibility(View.VISIBLE);
            binding.emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
