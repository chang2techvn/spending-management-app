package com.example.spending_management_app.presentation.fragment.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.spending_management_app.domain.model.Transaction;
import com.example.spending_management_app.presentation.activity.MainActivity;
import com.example.spending_management_app.R;
import com.example.spending_management_app.data.local.entity.CategoryBudgetEntity;
import com.example.spending_management_app.databinding.FragmentHomeBinding;

import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.local.entity.BudgetEntity;
import com.example.spending_management_app.data.local.entity.TransactionEntity;
import com.example.spending_management_app.presentation.viewmodel.home.HomeViewModel;

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
        
        // Load category spending data from database
        loadCategorySpendingFromDatabase();
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
                
                // Get total expense for THIS MONTH ONLY (not all time)
                Long totalExpense = AppDatabase.getInstance(getContext())
                        .transactionDao()
                        .getTotalExpenseByDateRange(startOfMonth, endOfMonth);
                
                android.util.Log.d("HomeFragment", "Monthly expense from " + startOfMonth + " to " + endOfMonth + ": " + totalExpense);
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Get monthly budget value
                        long budgetValue = 0;
                        if (monthlyBudgets != null && !monthlyBudgets.isEmpty()) {
                            BudgetEntity budget = monthlyBudgets.get(0);
                            budgetValue = budget.getMonthlyLimit();
                            binding.monthlyIncome.setText(String.format(Locale.getDefault(), "%,d", budgetValue) + " VND");
                            android.util.Log.d("HomeFragment", "Budget displayed: " + budgetValue);
                        } else {
                            binding.monthlyIncome.setText("Chưa thiết lập");
                            android.util.Log.d("HomeFragment", "No budget found - displaying 'Chưa thiết lập'");
                        }
                        
                        // Set monthly expense (absolute value, should be negative)
                        long expenseValue = totalExpense != null ? Math.abs(totalExpense) : 0;
                        binding.monthlyExpense.setText(String.format(Locale.getDefault(), "-%,d", expenseValue) + " VND");
                        
                        // Calculate and set remaining balance (budget - expense)
                        long remainingBalance = budgetValue - expenseValue;
                        binding.currentBalance.setText(String.format(Locale.getDefault(), "%,d", remainingBalance) + " VND");
                        
                        android.util.Log.d("HomeFragment", "Balance updated - Budget: " + budgetValue + 
                                ", Expense: " + expenseValue + ", Remaining: " + remainingBalance);
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
                        if (!isAdded() || getContext() == null) {
                            android.util.Log.w("HomeFragment", "Fragment not added or context null, skipping UI update");
                            return;
                        }
                        
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
                            android.util.Log.d("HomeFragment", "Created new adapter with " + recentTransactions.size() + " transactions");
                        } else {
                            // Update adapter data and notify
                            transactionAdapter.updateTransactions(recentTransactions);
                            android.util.Log.d("HomeFragment", "Updated adapter with " + recentTransactions.size() + " transactions");
                        }
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

    private void loadCategorySpendingFromDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Calculate month range
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
                
                // Get all transactions for this month by category
                List<TransactionEntity> allTransactions = AppDatabase.getInstance(getContext())
                        .transactionDao()
                        .getTransactionsByDateRange(startOfMonth, endOfMonth);
                
                // Get all category budgets for this month
                List<CategoryBudgetEntity> categoryBudgets =
                        AppDatabase.getInstance(getContext()).categoryBudgetDao()
                                .getAllCategoryBudgetsForMonth(startOfMonth, endOfMonth);
                
                // Calculate spending by category
                java.util.Map<String, Long> categorySpending = new java.util.HashMap<>();
                long totalSpending = 0;
                
                for (TransactionEntity transaction : allTransactions) {
                    if ("expense".equals(transaction.type)) {
                        long amount = Math.abs(transaction.amount);
                        categorySpending.put(transaction.category, 
                                categorySpending.getOrDefault(transaction.category, 0L) + amount);
                        totalSpending += amount;
                    }
                }
                
                // Create map of category budgets
                java.util.Map<String, Long> budgetMap = new java.util.HashMap<>();
                for (CategoryBudgetEntity budget : categoryBudgets) {
                    budgetMap.put(budget.getCategory(), budget.getBudgetAmount());
                }
                
                // Get all categories (with spending OR budget)
                // Inner class must be static to avoid issues with field access
                class CategoryData {
                    public String category;
                    public long spending;
                    public long budget;
                    
                    CategoryData(String category, long spending, long budget) {
                        this.category = category;
                        this.spending = spending;
                        this.budget = budget;
                    }
                }
                
                // Collect all unique categories (from spending or budgets)
                java.util.Set<String> allCategories = new java.util.HashSet<>();
                allCategories.addAll(categorySpending.keySet()); // Categories with spending
                allCategories.addAll(budgetMap.keySet()); // Categories with budgets
                
                List<CategoryData> categoryDataList = new ArrayList<>();
                for (String category : allCategories) {
                    long spending = categorySpending.getOrDefault(category, 0L);
                    long budget = budgetMap.getOrDefault(category, 0L);
                    categoryDataList.add(new CategoryData(category, spending, budget));
                }
                
                android.util.Log.d("HomeFragment", "Total categories found: " + categoryDataList.size());
                
                // Sort by spending amount (highest to lowest) - this determines the percentage order
                categoryDataList.sort((a, b) -> {
                    // Priority 1: Higher spending (determines percentage)
                    if (a.spending != b.spending) {
                        return Long.compare(b.spending, a.spending);
                    }
                    
                    // Priority 2: Has budget
                    if (a.budget > 0 && b.budget == 0) return -1;
                    if (a.budget == 0 && b.budget > 0) return 1;
                    
                    // Priority 3: Higher budget
                    return Long.compare(b.budget, a.budget);
                });
                
                // Take ALL categories (not just top 3)
                List<CategoryData> allCategoryData = new ArrayList<>(categoryDataList);
                
                // Calculate total spending of displayed categories (for percentage calculation)
                long totalCategorySpending = 0;
                for (CategoryData data : allCategoryData) {
                    totalCategorySpending += data.spending;
                }
                
                for (int i = 0; i < allCategoryData.size(); i++) {
                    android.util.Log.d("HomeFragment", "Category " + (i+1) + ": " + 
                            allCategoryData.get(i).category + 
                            " - Spending: " + allCategoryData.get(i).spending + 
                            ", Budget: " + allCategoryData.get(i).budget);
                }
                
                android.util.Log.d("HomeFragment", "Total category spending for percentage: " + totalCategorySpending);
                
                long finalTotalCategorySpending = totalCategorySpending;
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateCategoryUI(allCategoryData, finalTotalCategorySpending);
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error loading category spending", e);
            }
        });
    }
    
    private void updateCategoryUI(List<?> allCategories, long totalSpending) {
        android.util.Log.d("HomeFragment", "updateCategoryUI called with " + allCategories.size() + " categories");
        
        // Clear existing category views (except the title)
        ViewGroup container = binding.categoriesContainer;
        
        if (container == null) {
            android.util.Log.e("HomeFragment", "categoriesContainer is NULL!");
            return;
        }
        
        android.util.Log.d("HomeFragment", "Container found, child count: " + container.getChildCount());
        
        // Remove all views except the first one (title)
        int childCount = container.getChildCount();
        if (childCount > 1) {
            container.removeViews(1, childCount - 1);
            android.util.Log.d("HomeFragment", "Removed " + (childCount - 1) + " old views");
        }
        
        // Add each category dynamically
        for (int i = 0; i < allCategories.size(); i++) {
            Object obj = allCategories.get(i);
            
            try {
                // Access public fields directly
                String category = (String) obj.getClass().getField("category").get(obj);
                Long spendingLong = (Long) obj.getClass().getField("spending").get(obj);
                Long budgetLong = (Long) obj.getClass().getField("budget").get(obj);
                
                long spending = (spendingLong != null) ? spendingLong : 0L;
                long budget = (budgetLong != null) ? budgetLong : 0L;
                
                android.util.Log.d("HomeFragment", "Adding category: " + category + 
                        ", spending=" + spending + ", budget=" + budget);
                
                // Create category view
                View categoryView = createCategoryView(category, spending, budget, totalSpending);
                container.addView(categoryView);
                
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error creating category view", e);
                e.printStackTrace();
            }
        }
        
        android.util.Log.d("HomeFragment", "Total views in container: " + container.getChildCount());
    }
    
    private View createCategoryView(String category, long spending, long budget, long totalSpending) {
        // Create container
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        container.setLayoutParams(containerParams);
        
        // Create header (name + percentage)
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerParams.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        header.setLayoutParams(headerParams);
        
        // Category name
        android.widget.TextView nameView = new android.widget.TextView(getContext());
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        nameView.setLayoutParams(nameParams);
        String icon = getIconEmojiForCategory(category);
        nameView.setText(icon + " " + category);
        nameView.setTextColor(0xFF212121);
        nameView.setTextSize(14);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // Percentage with 1 decimal place for accuracy
        android.widget.TextView percentageView = new android.widget.TextView(getContext());
        percentageView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
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
        
        // Progress bar
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(
                getContext(), null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (8 * getResources().getDisplayMetrics().density)
        );
        progressParams.bottomMargin = (int) (4 * getResources().getDisplayMetrics().density);
        progressBar.setLayoutParams(progressParams);
        
        // Set rounded progress bar drawable
        progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.rounded_progress_bar, null));
        
        // Calculate progress percentage
        int progress = (budget > 0) ? (int)((spending * 100) / budget) : 0;
        progress = Math.min(progress, 100);
        progressBar.setProgress(progress);
        progressBar.setMax(100);
        
        // Set color based on spending percentage
        int progressColor;
        if (budget > 0) {
            if (progress < 70) {
                progressColor = 0xFF4CAF50; // Green - safe
            } else if (progress < 90) {
                progressColor = 0xFFFF9800; // Orange - warning
            } else {
                progressColor = 0xFFF44336; // Red - danger
            }
        } else {
            progressColor = 0xFF9E9E9E; // Gray - no budget set
        }
        
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(progressColor));
        progressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
        
        // Amount text
        android.widget.TextView amountView = new android.widget.TextView(getContext());
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        amountParams.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        amountView.setLayoutParams(amountParams);
        
        if (budget > 0) {
            amountView.setText(String.format(Locale.getDefault(), 
                    "%,d/%,d VND", spending, budget));
        } else {
            amountView.setText(String.format(Locale.getDefault(), 
                    "%,d VND (Chưa đặt ngân sách)", spending));
        }
        amountView.setTextColor(0xFF757575);
        amountView.setTextSize(12);
        
        // Add all views to container
        container.addView(header);
        container.addView(progressBar);
        container.addView(amountView);
        
        return container;
    }
    
    private String getIconEmojiForCategory(String category) {
        switch (category) {
            case "Ăn uống": return "🍽️";
            case "Di chuyển": return "🚗";
            case "Tiện ích": return "⚡";
            case "Y tế": return "🏥";
            case "Nhà ở": return "🏠";
            case "Mua sắm": return "🛍️";
            case "Giáo dục": return "📚";
            case "Sách & Học tập": return "📖";
            case "Thể thao": return "⚽";
            case "Sức khỏe & Làm đẹp": return "💆";
            case "Giải trí": return "🎬";
            case "Du lịch": return "✈️";
            case "Ăn ngoài & Cafe": return "☕";
            case "Quà tặng & Từ thiện": return "🎁";
            case "Hội họp & Tiệc tụng": return "🎉";
            case "Điện thoại & Internet": return "📱";
            case "Đăng ký & Dịch vụ": return "💳";
            case "Phần mềm & Apps": return "💻";
            case "Ngân hàng & Phí": return "🏦";
            case "Con cái": return "👶";
            case "Thú cưng": return "🐕";
            case "Gia đình": return "👨‍👩‍👧‍👦";
            case "Lương": return "💰";
            case "Đầu tư": return "📈";
            case "Thu nhập phụ": return "💵";
            case "Tiết kiệm": return "🏦";
            case "Khác": return "📌";
            default: return "💳";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    // Method to refresh recent transactions when new ones are added
    public void refreshRecentTransactions() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (isAdded() && getContext() != null) {
                    android.util.Log.d("HomeFragment", "refreshRecentTransactions called");
                    loadRecentTransactionsFromDatabase();
                    loadBalanceDataFromDatabase();
                    loadCategorySpendingFromDatabase(); // Also refresh category spending
                }
            });
        }
    }
    
    // Method to be called when the fragment becomes visible
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        loadRecentTransactionsFromDatabase();
        loadBalanceDataFromDatabase();
        loadCategorySpendingFromDatabase();
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