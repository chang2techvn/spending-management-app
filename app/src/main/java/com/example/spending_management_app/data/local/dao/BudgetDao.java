package com.example.spending_management_app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.spending_management_app.data.local.entity.BudgetEntity;

import java.util.List;

@Dao
public interface BudgetDao {
    @Insert
    void insert(BudgetEntity budget);

    @Update
    void update(BudgetEntity budget);
    
    @Delete
    void delete(BudgetEntity budget);

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    List<BudgetEntity> getAllBudgets(int userId);

    @Query("SELECT * FROM budgets WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    List<BudgetEntity> getBudgetsByDateRange(int userId, java.util.Date startDate, java.util.Date endDate);
    
    @Query("SELECT * FROM budgets WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    List<BudgetEntity> getBudgetsByDateRangeOrdered(int userId, java.util.Date startDate, java.util.Date endDate);
    
    @Query("DELETE FROM budgets WHERE userId = :userId AND date >= :startDate AND date <= :endDate")
    void deleteBudgetsByDateRange(int userId, java.util.Date startDate, java.util.Date endDate);

    // Get total budget of all months
    @Query("SELECT SUM(monthlyLimit) FROM budgets WHERE userId = :userId")
    Long getTotalBudget(int userId);

    // Get total budget of all months (LiveData for real-time updates)
    @Query("SELECT SUM(monthlyLimit) FROM budgets WHERE userId = :userId")
    LiveData<Long> getTotalBudgetLive(int userId);
    
    // Get total budget by date range
    @Query("SELECT SUM(monthlyLimit) FROM budgets WHERE userId = :userId AND date >= :startDate AND date <= :endDate")
    Long getTotalBudgetByDateRange(int userId, java.util.Date startDate, java.util.Date endDate);
}