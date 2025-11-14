package com.example.spending_management_app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(TransactionEntity transaction);

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
}