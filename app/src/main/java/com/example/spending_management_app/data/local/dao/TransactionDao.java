package com.example.spending_management_app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.spending_management_app.data.local.entity.CategorySummary;
import com.example.spending_management_app.data.local.entity.MonthlySpending;
import com.example.spending_management_app.data.local.entity.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(TransactionEntity transaction);
    
    @Update
    void update(TransactionEntity transaction);
    
    @Delete
    void delete(TransactionEntity transaction);
    
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    TransactionEntity getTransactionById(int id);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    List<TransactionEntity> getAllTransactions();

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    List<TransactionEntity> getRecentTransactions(int limit);

    @Query("SELECT * FROM transactions WHERE type = 'expense' ORDER BY date DESC")
    List<TransactionEntity> getAllExpenses();

    @Query("SELECT * FROM transactions WHERE type = 'income' ORDER BY date DESC")
    List<TransactionEntity> getAllIncomes();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'income'")
    Long getTotalIncome();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'expense'")
    Long getTotalExpense();

    // New methods for AI analysis
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<TransactionEntity> getTransactionsByDateRange(java.util.Date startDate, java.util.Date endDate);

    @Query("SELECT * FROM transactions WHERE type = 'expense' AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<TransactionEntity> getExpensesByDateRange(java.util.Date startDate, java.util.Date endDate);

    @Query("SELECT * FROM transactions WHERE type = 'income' AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<TransactionEntity> getIncomesByDateRange(java.util.Date startDate, java.util.Date endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'expense' AND date BETWEEN :startDate AND :endDate")
    Long getTotalExpenseByDateRange(java.util.Date startDate, java.util.Date endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'income' AND date BETWEEN :startDate AND :endDate")
    Long getTotalIncomeByDateRange(java.util.Date startDate, java.util.Date endDate);

    @Query("SELECT * FROM transactions WHERE date(date) = date(:specificDate) ORDER BY date DESC")
    List<TransactionEntity> getTransactionsBySpecificDate(java.util.Date specificDate);

    @Query("SELECT COUNT(*) FROM transactions WHERE type = 'expense' AND date BETWEEN :startDate AND :endDate")
    int getExpenseCountByDateRange(java.util.Date startDate, java.util.Date endDate);

    @Query("SELECT category, SUM(ABS(amount)) as total FROM transactions WHERE type = 'expense' AND date BETWEEN :startDate AND :endDate GROUP BY category ORDER BY total DESC")
    List<CategorySummary> getExpensesByCategory(java.util.Date startDate, java.util.Date endDate);

    // Get monthly spending for chart (non-observable)
    @Query("SELECT strftime('%Y-%m', date / 1000, 'unixepoch') as month, SUM(ABS(amount)) as total " +
           "FROM transactions WHERE type = 'expense' " +
           "GROUP BY month ORDER BY month ASC")
    List<MonthlySpending> getMonthlySpending();

    // Get monthly spending for chart (LiveData for real-time updates)
    @Query("SELECT strftime('%Y-%m', date / 1000, 'unixepoch') as month, SUM(ABS(amount)) as total " +
           "FROM transactions WHERE type = 'expense' " +
           "GROUP BY month ORDER BY month ASC")
    LiveData<List<MonthlySpending>> getMonthlySpendingLive();

    // Get monthly spending by year (LiveData for real-time updates)
    @Query("SELECT strftime('%Y-%m', date / 1000, 'unixepoch') as month, SUM(ABS(amount)) as total " +
           "FROM transactions WHERE type = 'expense' " +
           "AND strftime('%Y', date / 1000, 'unixepoch') = :year " +
           "GROUP BY month ORDER BY month ASC")
    LiveData<List<MonthlySpending>> getMonthlySpendingByYearLive(String year);

    // Get total income/expense as LiveData for real-time updates
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'income'")
    LiveData<Long> getTotalIncomeLive();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'expense'")
    LiveData<Long> getTotalExpenseLive();

    // Get distinct years from transactions for year dropdown
    @Query("SELECT DISTINCT strftime('%Y', date / 1000, 'unixepoch') as year " +
           "FROM transactions WHERE type = 'expense' " +
           "ORDER BY year DESC")
    List<String> getDistinctYears();
}