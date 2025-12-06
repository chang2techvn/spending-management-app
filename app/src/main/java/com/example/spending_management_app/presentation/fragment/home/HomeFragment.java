package com.example.spending_management_app.presentation.fragment.home;

import android.content.SharedPreferences;
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
import com.example.spending_management_app.utils.CategoryUtils;
import com.example.spending_management_app.utils.ToastHelper;

import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.local.entity.BudgetEntity;
import com.example.spending_management_app.data.local.entity.TransactionEntity;
import com.example.spending_management_app.presentation.viewmodel.home.HomeViewModel;
import com.example.spending_management_app.utils.CurrencyFormatter;
import com.example.spending_management_app.utils.UserSession;

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
    private UserSession userSession;
    private static final String PREFS_NAME = "BudgetWarnings";
    private static final String KEY_WARNED_CATEGORIES = "warned_categories_";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize UserSession
        userSession = UserSession.getInstance(requireContext());

        // Setup balance data
        setupBalanceData();

        // Setup quick actions
        setupQuickActions();

        // Setup recent transactions from database
        loadRecentTransactionsFromDatabase();

        // Setup month comparison
        loadMonthComparison();

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

                int userId = userSession.getCurrentUserId();
                
                // Get monthly budget using improved query
                List<BudgetEntity> monthlyBudgets = AppDatabase.getInstance(getContext())
                        .budgetDao().getBudgetsByDateRangeOrdered(userId, startOfMonth, endOfMonth);
                
                android.util.Log.d("HomeFragment", "Found " + (monthlyBudgets != null ? monthlyBudgets.size() : 0) + " budgets for userId: " + userId);
                if (monthlyBudgets != null) {
                    for (int i = 0; i < monthlyBudgets.size(); i++) {
                        BudgetEntity b = monthlyBudgets.get(i);
                        android.util.Log.d("HomeFragment", "Budget " + i + ": date=" + b.date + ", amount=" + b.monthlyLimit);
                    }
                }
                
                // Get total expense for THIS MONTH ONLY (not all time)
                Long totalExpense = AppDatabase.getInstance(getContext())
                        .transactionDao()
                        .getTotalExpenseByDateRange(userId, startOfMonth, endOfMonth);
                
                android.util.Log.d("HomeFragment", "Monthly expense from " + startOfMonth + " to " + endOfMonth + ": " + totalExpense);
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Get monthly budget value
                        long budgetValue = 0;
                        if (monthlyBudgets != null && !monthlyBudgets.isEmpty()) {
                            BudgetEntity budget = monthlyBudgets.get(0);
                            budgetValue = budget.getMonthlyLimit();
                            binding.monthlyIncome.setText(CurrencyFormatter.formatCurrency(getContext(), budgetValue));
                            android.util.Log.d("HomeFragment", "Budget displayed: " + budgetValue);
                        } else {
                            binding.monthlyIncome.setText(getString(R.string.not_set));
                            android.util.Log.d("HomeFragment", "No budget found - displaying '" + getString(R.string.not_set) + "'");
                        }
                        
                        // Set monthly expense (absolute value, should be negative)
                        long expenseValue = totalExpense != null ? Math.abs(totalExpense) : 0;
                        binding.monthlyExpense.setText("-" + CurrencyFormatter.formatCurrency(getContext(), expenseValue));
                        
                        // Calculate and set remaining balance (budget - expense)
                        long remainingBalance = budgetValue - expenseValue;
                        binding.currentBalance.setText(CurrencyFormatter.formatCurrency(getContext(), remainingBalance));
                        
                        // Show warning toast if expense exceeds budget (only once per month)
                        if (budgetValue > 0 && expenseValue > budgetValue) {
                            Calendar calForKey = Calendar.getInstance();
                            String monthKey = calForKey.get(Calendar.YEAR) + "_" + (calForKey.get(Calendar.MONTH) + 1);
                            String monthlyBudgetKey = "monthly_budget_" + monthKey;
                            
                            SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
                            boolean alreadyWarned = prefs.getBoolean(monthlyBudgetKey, false);
                            
                            if (!alreadyWarned && getActivity() != null) {
                                ToastHelper.showErrorToast(getActivity(), getString(R.string.budget_exceeded_warning));
                                prefs.edit().putBoolean(monthlyBudgetKey, true).apply();
                            }
                        }
                        
                        android.util.Log.d("HomeFragment", "Balance updated - Budget: " + budgetValue + 
                                ", Expense: " + expenseValue + ", Remaining: " + remainingBalance);
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error loading balance data", e);
                
                // Fallback to sample data
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        binding.monthlyIncome.setText(CurrencyFormatter.formatCurrency(getContext(), 12500000));
                        binding.monthlyExpense.setText("-" + CurrencyFormatter.formatCurrency(getContext(), 5500000));
                        binding.currentBalance.setText(CurrencyFormatter.formatCurrency(getContext(), 7000000));
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
                ((MainActivity) getActivity()).openAiChat(getString(R.string.add_income_chat_message));
            }

            @Override
            public void onSetBudgetSelected() {
                ((MainActivity) getActivity()).openAiChat(getString(R.string.set_budget_chat_message));
            }
        });
        dialog.show(getParentFragmentManager(), "BudgetManagementDialog");
    }

    private void showAddTransactionDialog() {
        ((MainActivity) getActivity()).openAiChat(getString(R.string.add_expense_chat_message));
    }

    private void openAiChat(String prompt) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openAiChat(prompt);
        }
    }

    private void loadRecentTransactionsFromDatabase() {
        // Initialize empty list first
        transactions = new ArrayList<>();

        // Show skeleton loading
        if (transactionAdapter == null) {
            transactionAdapter = new TransactionAdapter(transactions);
            binding.recentTransactionsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.recentTransactionsRecycler.setAdapter(transactionAdapter);

            // Setup view all transactions click
            binding.viewAllTransactions.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.navigation_history);
            });
        }
        transactionAdapter.setLoading(true);

        // Load data from database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int userId = userSession.getCurrentUserId();
                
                // Get recent transactions from database (limit to 5)
                List<TransactionEntity> transactionEntities = AppDatabase.getInstance(getContext())
                        .transactionDao()
                        .getRecentTransactions(userId, 5);
                
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

                        // Update adapter data and notify
                        transactionAdapter.updateTransactions(recentTransactions);
                        android.util.Log.d("HomeFragment", "Created new adapter with " + recentTransactions.size() + " transactions");
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
        if (transactionAdapter == null) {
            transactionAdapter = new TransactionAdapter(transactions);
            binding.recentTransactionsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.recentTransactionsRecycler.setAdapter(transactionAdapter);

            // Setup view all transactions click
            binding.viewAllTransactions.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.navigation_history);
            });
        } else {
            transactionAdapter.updateTransactions(transactions);
        }

        android.util.Log.d("HomeFragment", "Sample recent transactions loaded with " + transactions.size() + " items");
    }

    private void loadCategorySpendingFromDatabase() {
        android.util.Log.d("HomeFragment", "=== loadCategorySpendingFromDatabase START ===");
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
                
                android.util.Log.d("HomeFragment", "Loading category spending for month: " + startOfMonth + " to " + endOfMonth);
                
                int userId = userSession.getCurrentUserId();
                
                // Get all transactions for this month by category
                List<TransactionEntity> allTransactions = AppDatabase.getInstance(getContext())
                        .transactionDao()
                        .getTransactionsByDateRange(userId, startOfMonth, endOfMonth);
                
                android.util.Log.d("HomeFragment", "Found " + allTransactions.size() + " transactions in current month for userId: " + userId);
                for (TransactionEntity transaction : allTransactions) {
                    android.util.Log.d("HomeFragment", "Transaction: " + transaction.description + 
                            " - " + transaction.category + " - " + transaction.amount + " - " + transaction.date);
                }
                
                // Get all category budgets for this month
                List<CategoryBudgetEntity> categoryBudgets =
                        AppDatabase.getInstance(getContext()).categoryBudgetDao()
                                .getAllCategoryBudgetsForMonth(userId, startOfMonth, endOfMonth);
                
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
                
                // Get current month key for storing warnings
                Calendar calForKey = Calendar.getInstance();
                String monthKey = calForKey.get(Calendar.YEAR) + "_" + (calForKey.get(Calendar.MONTH) + 1);
                
                // Get SharedPreferences
                SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
                java.util.Set<String> warnedCategories = prefs.getStringSet(KEY_WARNED_CATEGORIES + monthKey, new java.util.HashSet<>());
                
                // Check for NEW category budget warnings
                List<String> newExceededCategories = new ArrayList<>();
                java.util.Set<String> currentExceededCategories = new java.util.HashSet<>();
                
                for (CategoryData data : allCategoryData) {
                    if (data.budget > 0 && data.spending > data.budget) {
                        currentExceededCategories.add(data.category);
                        
                        // Only warn if this category hasn't been warned before this month
                        if (!warnedCategories.contains(data.category)) {
                            String localizedName = CategoryUtils.getLocalizedCategoryName(getContext(), data.category);
                            newExceededCategories.add(localizedName);
                        }
                    }
                }
                
                // Update warned categories in SharedPreferences
                if (!newExceededCategories.isEmpty()) {
                    java.util.Set<String> updatedWarnedCategories = new java.util.HashSet<>(warnedCategories);
                    updatedWarnedCategories.addAll(currentExceededCategories);
                    prefs.edit().putStringSet(KEY_WARNED_CATEGORIES + monthKey, updatedWarnedCategories).apply();
                }
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.util.Log.d("HomeFragment", "Updating UI with " + allCategoryData.size() + " categories");
                        updateCategoryUI(allCategoryData, finalTotalCategorySpending);
                        
                        // Show warning toast ONLY for NEW exceeded category budgets
                        if (!newExceededCategories.isEmpty() && getActivity() != null) {
                            for (String categoryName : newExceededCategories) {
                                String message = getString(R.string.category_budget_exceeded_warning, categoryName);
                                ToastHelper.showErrorToast(getActivity(), message);
                            }
                        }
                    });
                }
                
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error loading category spending", e);
            }
        });
        android.util.Log.d("HomeFragment", "=== loadCategorySpendingFromDatabase END ===");
    }
    
    private void updateCategoryUI(List<?> allCategories, long totalSpending) {
        android.util.Log.d("HomeFragment", "=== updateCategoryUI START ===");
        android.util.Log.d("HomeFragment", "updateCategoryUI called with " + allCategories.size() + " categories, totalSpending=" + totalSpending);
        
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
        android.util.Log.d("HomeFragment", "=== updateCategoryUI END ===");
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
    String localizedName = CategoryUtils.getLocalizedCategoryName(getContext(), category);
    String icon = CategoryUtils.getIconForCategory(localizedName);
    nameView.setText(icon + " " + localizedName);
        nameView.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.text_primary));
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
            amountView.setText(CurrencyFormatter.formatCurrency(getContext(), spending) + "/" + CurrencyFormatter.formatCurrency(getContext(), budget));
        } else {
            amountView.setText(CurrencyFormatter.formatCurrency(getContext(), spending) + " (" + getString(R.string.budget_not_set_context) + ")");
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
        String localized = CategoryUtils.getLocalizedCategoryName(getContext(), category);
        return CategoryUtils.getIconForCategory(localized);
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
                    loadMonthComparison(); // Also refresh month comparison
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
        loadMonthComparison();
    }

    private void loadMonthComparison() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Calendar cal = Calendar.getInstance();

                // Calculate THIS MONTH range
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                java.util.Date startOfThisMonth = cal.getTime();

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                java.util.Date endOfThisMonth = cal.getTime();

                // Calculate LAST MONTH range
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.add(Calendar.MONTH, -1); // Go back 1 month
                java.util.Date startOfLastMonth = cal.getTime();

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                java.util.Date endOfLastMonth = cal.getTime();

                int userId = userSession.getCurrentUserId();

                // Get this month spending
                Long thisMonthSpendingLong = AppDatabase.getInstance(getContext())
                        .transactionDao().getTotalExpenseByDateRange(userId, startOfThisMonth, endOfThisMonth);
                long thisMonthSpending = (thisMonthSpendingLong != null) ? Math.abs(thisMonthSpendingLong) : 0;

                // Get last month spending
                Long lastMonthSpendingLong = AppDatabase.getInstance(getContext())
                        .transactionDao().getTotalExpenseByDateRange(userId, startOfLastMonth, endOfLastMonth);
                long lastMonthSpending = (lastMonthSpendingLong != null) ? Math.abs(lastMonthSpendingLong) : 0;

                // Calculate difference
                long difference = thisMonthSpending - lastMonthSpending;

                android.util.Log.d("HomeFragment", "This month spending: " + thisMonthSpending);
                android.util.Log.d("HomeFragment", "Last month spending: " + lastMonthSpending);
                android.util.Log.d("HomeFragment", "Difference: " + difference);

                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateMonthComparisonUI(lastMonthSpending, thisMonthSpending, difference);
                    });
                }

            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error loading month comparison", e);
            }
        });
    }

    private void updateMonthComparisonUI(long lastMonthSpending, long thisMonthSpending, long difference) {
        // Update last month spending
        binding.lastMonthSpending.setText(CurrencyFormatter.formatCurrencyShort(getContext(), lastMonthSpending));

        // Update this month spending
        binding.thisMonthSpending.setText(CurrencyFormatter.formatCurrencyShort(getContext(), thisMonthSpending));

        // Update difference with color
        String differenceText;
        int differenceColor;

        if (difference > 0) {
            // Spending increased (bad)
            differenceText = "+" + CurrencyFormatter.formatCurrencyShort(getContext(), difference);
            differenceColor = 0xFFF44336; // Red
        } else if (difference < 0) {
            // Spending decreased (good)
            differenceText = "-" + CurrencyFormatter.formatCurrencyShort(getContext(), Math.abs(difference));
            differenceColor = 0xFF4CAF50; // Green
        } else {
            // No change
            differenceText = "0";
            differenceColor = 0xFF757575; // Gray
        }

        binding.differenceSpending.setText(differenceText);
        binding.differenceSpending.setTextColor(differenceColor);

        android.util.Log.d("HomeFragment", "Month comparison UI updated");
    }

    // Helper method to get appropriate icon for category
    private String getIconForCategory(String category, String type) {
        if ("income".equals(type)) {
            return "ic_home";
        }
        String localized = CategoryUtils.getLocalizedCategoryName(getContext(), category);
        // Keep "income" behavior; for others return emoji/icon from CategoryUtils
        return CategoryUtils.getIconForCategory(localized);
    }

    private String getLocalizedCategoryName(String category) {
        return CategoryUtils.getLocalizedCategoryName(getContext(), category);
    }
}