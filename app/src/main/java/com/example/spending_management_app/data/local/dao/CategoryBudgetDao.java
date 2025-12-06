package com.example.spending_management_app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.spending_management_app.data.local.entity.CategoryBudgetEntity;

import java.util.Date;
import java.util.List;

@Dao
public interface CategoryBudgetDao {
    @Insert
    long insert(CategoryBudgetEntity categoryBudget);
    
    @Update
    void update(CategoryBudgetEntity categoryBudget);
    
    @Delete
    void delete(CategoryBudgetEntity categoryBudget);
    
    @Query("SELECT * FROM category_budgets WHERE userId = :userId AND category = :category AND date >= :startDate AND date <= :endDate ORDER BY date DESC LIMIT 1")
    CategoryBudgetEntity getCategoryBudgetForMonth(int userId, String category, Date startDate, Date endDate);
    
    @Query("SELECT * FROM category_budgets WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY category ASC")
    List<CategoryBudgetEntity> getAllCategoryBudgetsForMonth(int userId, Date startDate, Date endDate);
    
    @Query("SELECT DISTINCT category FROM category_budgets WHERE userId = :userId ORDER BY category ASC")
    List<String> getAllCategories(int userId);
    
    @Query("DELETE FROM category_budgets WHERE userId = :userId AND date >= :startDate AND date <= :endDate")
    void deleteAllForMonth(int userId, Date startDate, Date endDate);
}
