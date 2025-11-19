package com.example.spending_management_app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BudgetDao {
    @Insert
    void insert(BudgetEntity budget);

    @Update
    void update(BudgetEntity budget);

    @Query("SELECT * FROM budgets")
    List<BudgetEntity> getAllBudgets();

    @Query("SELECT * FROM budgets WHERE date BETWEEN :startDate AND :endDate")
    List<BudgetEntity> getBudgetsByDateRange(java.util.Date startDate, java.util.Date endDate);
    
    @Query("SELECT * FROM budgets WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    List<BudgetEntity> getBudgetsByDateRangeOrdered(java.util.Date startDate, java.util.Date endDate);
    
    @Query("DELETE FROM budgets WHERE date >= :startDate AND date <= :endDate")
    void deleteBudgetsByDateRange(java.util.Date startDate, java.util.Date endDate);

    // Get total budget of all months
    @Query("SELECT SUM(monthlyLimit) FROM budgets")
    Long getTotalBudget();

    // Get total budget of all months (LiveData for real-time updates)
    @Query("SELECT SUM(monthlyLimit) FROM budgets")
    LiveData<Long> getTotalBudgetLive();
    
    // Get total budget by date range
    @Query("SELECT SUM(monthlyLimit) FROM budgets WHERE date >= :startDate AND date <= :endDate")
    Long getTotalBudgetByDateRange(java.util.Date startDate, java.util.Date endDate);
}