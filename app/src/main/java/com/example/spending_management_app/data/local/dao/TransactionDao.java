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
    
    @Query("SELECT * FROM transactions WHERE userId = :userId AND id = :id LIMIT 1")
    TransactionEntity getTransactionById(int userId, int id);

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    List<TransactionEntity> getAllTransactions(int userId);

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    List<TransactionEntity> getRecentTransactions(int userId, int limit);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = 'expense' ORDER BY date DESC")
    List<TransactionEntity> getAllExpenses(int userId);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = 'income' ORDER BY date DESC")
    List<TransactionEntity> getAllIncomes(int userId);

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'income'")
    Long getTotalIncome(int userId);

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'expense'")
    Long getTotalExpense(int userId);

    // New methods for AI analysis
    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<TransactionEntity> getTransactionsByDateRange(int userId, java.util.Date startDate, java.util.Date endDate);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = 'expense' AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<TransactionEntity> getExpensesByDateRange(int userId, java.util.Date startDate, java.util.Date endDate);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = 'income' AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<TransactionEntity> getIncomesByDateRange(int userId, java.util.Date startDate, java.util.Date endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'expense' AND date BETWEEN :startDate AND :endDate")
    Long getTotalExpenseByDateRange(int userId, java.util.Date startDate, java.util.Date endDate);

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'income' AND date BETWEEN :startDate AND :endDate")
    Long getTotalIncomeByDateRange(int userId, java.util.Date startDate, java.util.Date endDate);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date(date) = date(:specificDate) ORDER BY date DESC")
    List<TransactionEntity> getTransactionsBySpecificDate(int userId, java.util.Date specificDate);

    @Query("SELECT COUNT(*) FROM transactions WHERE userId = :userId AND type = 'expense' AND date BETWEEN :startDate AND :endDate")
    int getExpenseCountByDateRange(int userId, java.util.Date startDate, java.util.Date endDate);

    @Query("SELECT category, SUM(ABS(amount)) as total FROM transactions WHERE userId = :userId AND type = 'expense' AND date BETWEEN :startDate AND :endDate GROUP BY category ORDER BY total DESC")
    List<CategorySummary> getExpensesByCategory(int userId, java.util.Date startDate, java.util.Date endDate);

    // Get monthly spending for chart (non-observable)
    @Query("SELECT strftime('%Y-%m', date / 1000, 'unixepoch') as month, SUM(ABS(amount)) as total " +
           "FROM transactions WHERE userId = :userId AND type = 'expense' " +
           "GROUP BY month ORDER BY month ASC")
    List<MonthlySpending> getMonthlySpending(int userId);

    // Get monthly spending for chart (LiveData for real-time updates)
    @Query("SELECT strftime('%Y-%m', date / 1000, 'unixepoch') as month, SUM(ABS(amount)) as total " +
           "FROM transactions WHERE userId = :userId AND type = 'expense' " +
           "GROUP BY month ORDER BY month ASC")
    LiveData<List<MonthlySpending>> getMonthlySpendingLive(int userId);

    // Get monthly spending by year (LiveData for real-time updates)
    @Query("SELECT strftime('%Y-%m', date / 1000, 'unixepoch') as month, SUM(ABS(amount)) as total " +
           "FROM transactions WHERE userId = :userId AND type = 'expense' " +
           "AND strftime('%Y', date / 1000, 'unixepoch') = :year " +
           "GROUP BY month ORDER BY month ASC")
    LiveData<List<MonthlySpending>> getMonthlySpendingByYearLive(int userId, String year);

    // Get total income/expense as LiveData for real-time updates
    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'income'")
    LiveData<Long> getTotalIncomeLive(int userId);

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'expense'")
    LiveData<Long> getTotalExpenseLive(int userId);

    // Get distinct years from transactions for year dropdown
    @Query("SELECT DISTINCT strftime('%Y', date / 1000, 'unixepoch') as year " +
           "FROM transactions WHERE userId = :userId AND type = 'expense' " +
           "ORDER BY year DESC")
    List<String> getDistinctYears(int userId);
}