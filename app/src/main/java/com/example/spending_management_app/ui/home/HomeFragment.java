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

import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.BudgetEntity;
import com.example.spending_management_app.database.TransactionEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

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

        // Setup recent transactions from database
        loadRecentTransactionsFromDatabase();

        return root;
    }

    private void setupBalanceData() {
        // Load balance data from database
        loadBalanceDataFromDatabase();
    }
    
    private void loadBalanceDataFromDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Calculate month range with precise time
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date startOfMonth = cal.getTime();
                
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                Date endOfMonth = cal.getTime();

                android.util.Log.d("HomeFragment", "Loading budget for range: " + startOfMonth + " to " + endOfMonth);

                // Get monthly budget using improved query
                List<BudgetEntity> monthlyBudgets = AppDatabase.getInstance(getContext())
                        .budgetDao().getBudgetsByDateRangeOrdered(startOfMonth, endOfMonth);
                
                android.util.Log.d("HomeFragment", "Found " + (monthlyBudgets != null ? monthlyBudgets.size() : 0) + " budgets");
                if (monthlyBudgets != null) {
                    for (int i = 0; i < monthlyBudgets.size(); i++) {
                        BudgetEntity b = monthlyBudgets.get(i);
                        android.util.Log.d("HomeFragment", "Budget " + i + ": date=" + b.date + ", amount=" + b.monthlyLimit);
                    }
                }
                
                // Get total income and expenses from database
                Long totalIncome = AppDatabase.getInstance(getContext()).transactionDao().getTotalIncome();
                Long totalExpense = AppDatabase.getInstance(getContext()).transactionDao().getTotalExpense();
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Set monthly budget (income)
                        if (monthlyBudgets != null && !monthlyBudgets.isEmpty()) {
                            BudgetEntity budget = monthlyBudgets.get(0);
                            long budgetValue = budget.getMonthlyLimit();
                            binding.monthlyIncome.setText(String.format(Locale.getDefault(), "%,d", budgetValue) + " VND");
                            android.util.Log.d("HomeFragment", "Budget displayed: " + budgetValue);
                        } else {
                            binding.monthlyIncome.setText("Chưa thiết lập");
                            android.util.Log.d("HomeFragment", "No budget found - displaying 'Chưa thiết lập'");
                        }
                        
                        // Set total expense (absolute value, should be negative)
                        long expenseValue = totalExpense != null ? Math.abs(totalExpense) : 0;
                        binding.monthlyExpense.setText(String.format(Locale.getDefault(), "-%,d", expenseValue) + " VND");
                        
                        // Calculate and set current balance
                        long incomeValue = totalIncome != null ? totalIncome : 0;
                        long currentBalance = incomeValue + (totalExpense != null ? totalExpense : 0); // totalExpense is negative
                        binding.currentBalance.setText(String.format(Locale.getDefault(), "%,d", currentBalance) + " VND");
                        
                        android.util.Log.d("HomeFragment", "Balance updated - Income: " + incomeValue + 
                                ", Expense: " + expenseValue + ", Balance: " + currentBalance);
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error loading balance data", e);
                
                // Fallback to sample data
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        binding.monthlyIncome.setText("12,500,000 VND");
                        binding.monthlyExpense.setText("-5,500,000 VND");
                        binding.currentBalance.setText("7,000,000 VND");
                    });
                }
            }
        });
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

    private void loadRecentTransactionsFromDatabase() {
        // Initialize empty list first
        transactions = new ArrayList<>();
        
        // Load data from database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Get recent transactions from database (limit to 5)
                List<TransactionEntity> transactionEntities = AppDatabase.getInstance(getContext())
                        .transactionDao()
                        .getRecentTransactions(5);
                
                // Convert TransactionEntity to Transaction objects
                List<Transaction> recentTransactions = new ArrayList<>();
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
                    recentTransactions.add(transaction);
                }
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        transactions.clear();
                        transactions.addAll(recentTransactions);
                        
                        // Setup RecyclerView if not done yet
                        if (transactionAdapter == null) {
                            transactionAdapter = new TransactionAdapter(transactions);
                            binding.recentTransactionsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
                            binding.recentTransactionsRecycler.setAdapter(transactionAdapter);
                            
                            // Setup view all transactions click
                            binding.viewAllTransactions.setOnClickListener(v -> {
                                Navigation.findNavController(v).navigate(R.id.navigation_history);
                            });
                        } else {
                            // Just notify adapter of data change
                            transactionAdapter.notifyDataSetChanged();
                        }
                        
                        android.util.Log.d("HomeFragment", "Loaded " + recentTransactions.size() + " recent transactions from database");
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error loading recent transactions from database", e);
                
                // Fallback to sample data on error
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        setupSampleRecentTransactions();
                    });
                }
            }
        });
    }

    private void setupSampleRecentTransactions() {
        // Fallback sample data (keep original method name for compatibility)
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
        
        android.util.Log.d("HomeFragment", "Sample recent transactions loaded with " + transactions.size() + " items");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    // Method to refresh recent transactions when new ones are added
    public void refreshRecentTransactions() {
        if (isAdded() && getContext() != null) {
            loadRecentTransactionsFromDatabase();
            loadBalanceDataFromDatabase(); // Also refresh balance
        }
    }
    
    // Method to be called when the fragment becomes visible
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadRecentTransactionsFromDatabase();
        loadBalanceDataFromDatabase();
    }
    
    // Helper method to get appropriate icon for category
    private String getIconForCategory(String category, String type) {
        if ("income".equals(type)) {
            return "ic_home_black_24dp";
        }
        
        switch (category) {
            case "Ăn uống":
                return "ic_restaurant";
            case "Di chuyển":
                return "ic_directions_car";
            case "Mua sắm":
                return "ic_shopping_cart";
            case "Ngân sách":
                return "ic_account_balance_wallet";
            case "Tiện ích":
                return "ic_electrical_services";
            case "Giáo dục":
                return "ic_school";
            case "Giải trí":
                return "ic_local_movies";
            case "Y tế":
                return "ic_local_hospital";
            default:
                return "ic_bar_chart";
        }
    }
}