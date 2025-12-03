package com.example.spending_management_app.domain.repository;

import com.example.spending_management_app.data.local.entity.CategoryBudgetEntity;

import java.util.Date;
import java.util.List;

/**
 * Repository interface for category budget operations
 */
public interface CategoryBudgetRepository {
    List<CategoryBudgetEntity> getAllCategoryBudgetsForMonth(Date startDate, Date endDate);
    void delete(CategoryBudgetEntity categoryBudget);
    void deleteAllForMonth(Date startDate, Date endDate);
    CategoryBudgetEntity getCategoryBudgetForMonth(String category, Date startDate, Date endDate);
    void update(CategoryBudgetEntity categoryBudget);
    void insert(CategoryBudgetEntity categoryBudget);
}