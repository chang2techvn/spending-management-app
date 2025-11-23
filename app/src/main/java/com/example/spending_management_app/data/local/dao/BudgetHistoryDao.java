package com.example.spending_management_app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.spending_management_app.data.local.entity.BudgetHistoryEntity;

import java.util.Date;
import java.util.List;

@Dao
public interface BudgetHistoryDao {
    @Insert
    void insert(BudgetHistoryEntity budgetHistory);
    
    @Query("SELECT * FROM budget_history ORDER BY date DESC")
    List<BudgetHistoryEntity> getAllBudgetHistory();
    
    @Query("SELECT * FROM budget_history WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    List<BudgetHistoryEntity> getBudgetHistoryByDateRange(Date startDate, Date endDate);
    
    @Query("DELETE FROM budget_history")
    void deleteAll();
}
