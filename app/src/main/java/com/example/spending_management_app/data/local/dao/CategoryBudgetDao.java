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
    
    @Query("SELECT * FROM category_budgets WHERE category = :category AND date >= :startDate AND date <= :endDate ORDER BY date DESC LIMIT 1")
    CategoryBudgetEntity getCategoryBudgetForMonth(String category, Date startDate, Date endDate);
    
    @Query("SELECT * FROM category_budgets WHERE date >= :startDate AND date <= :endDate ORDER BY category ASC")
    List<CategoryBudgetEntity> getAllCategoryBudgetsForMonth(Date startDate, Date endDate);
    
    @Query("SELECT DISTINCT category FROM category_budgets ORDER BY category ASC")
    List<String> getAllCategories();
    
    @Query("DELETE FROM category_budgets WHERE date >= :startDate AND date <= :endDate")
    void deleteAllForMonth(Date startDate, Date endDate);
}
