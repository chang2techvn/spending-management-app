package com.example.spending_management_app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spending_management_app.MainActivity;
import com.example.spending_management_app.R;
import com.example.spending_management_app.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactions;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup balance data
        setupBalanceData();

        // Setup quick actions
        setupQuickActions();

        // Setup recent transactions
        setupRecentTransactions();

        return root;
    }

    private void setupBalanceData() {
        // Set current balance
        binding.currentBalance.setText("12,500,000 VND");

        // Set monthly income and expense
        binding.monthlyIncome.setText("+8,000,000");
        binding.monthlyExpense.setText("-5,500,000");
    }

    private void setupQuickActions() {
        binding.addIncomeBtn.setOnClickListener(v -> showBudgetManagementDialog());
        binding.addExpenseBtn.setOnClickListener(v -> showAddTransactionDialog());
    }

    private void showBudgetManagementDialog() {
        BudgetManagementDialog dialog = new BudgetManagementDialog();
        dialog.setOnActionSelectedListener(new BudgetManagementDialog.OnActionSelectedListener() {
            @Override
            public void onAddIncomeSelected() {
                ((MainActivity) getActivity()).openAiChat("Tôi muốn thêm thu nhập");
            }

            @Override
            public void onSetBudgetSelected() {
                ((MainActivity) getActivity()).openAiChat("Tôi muốn thiết lập ngân sách");
            }
        });
        dialog.show(getParentFragmentManager(), "BudgetManagementDialog");
    }

    private void showAddTransactionDialog() {
        ((MainActivity) getActivity()).openAiChat("Tôi muốn thêm chi tiêu");
    }

    private void openAiChat(String prompt) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openAiChat(prompt);
        }
    }

    private void setupRecentTransactions() {
        // Create sample transaction data
        transactions = new ArrayList<>();
        transactions.add(new Transaction("Ăn trưa tại quán cơm", "Ăn uống", -45000, "ic_bar_chart", new Date(), "expense"));
        transactions.add(new Transaction("Tiền xăng", "Di chuyển", -120000, "ic_bar_chart", new Date(), "expense"));
        transactions.add(new Transaction("Mua áo mới", "Mua sắm", -350000, "ic_bar_chart", new Date(), "expense"));
        transactions.add(new Transaction("Lương tháng 10", "Ngân sách", 8000000, "ic_home_black_24dp", new Date(), "income"));

        // Setup RecyclerView
        transactionAdapter = new TransactionAdapter(transactions);
        binding.recentTransactionsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recentTransactionsRecycler.setAdapter(transactionAdapter);

        // Setup view all transactions click
        binding.viewAllTransactions.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_history);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}