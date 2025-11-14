package com.example.spending_management_app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.spending_management_app.database.entity.BudgetEntity;

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
}