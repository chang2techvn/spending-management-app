package com.example.spending_management_app.data.repository;

import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.local.entity.CategoryBudgetEntity;
import com.example.spending_management_app.domain.repository.CategoryBudgetRepository;

import java.util.Date;
import java.util.List;

/**
 * Implementation of CategoryBudgetRepository
 */
public class CategoryBudgetRepositoryImpl implements CategoryBudgetRepository {

    private final AppDatabase appDatabase;

    public CategoryBudgetRepositoryImpl(AppDatabase appDatabase) {
        this.appDatabase = appDatabase;
    }

    @Override
    public List<CategoryBudgetEntity> getAllCategoryBudgetsForMonth(int userId, Date startDate, Date endDate) {
        return appDatabase.categoryBudgetDao().getAllCategoryBudgetsForMonth(userId, startDate, endDate);
    }

    @Override
    public void delete(CategoryBudgetEntity categoryBudget) {
        appDatabase.categoryBudgetDao().delete(categoryBudget);
    }

    @Override
    public void deleteAllForMonth(int userId, Date startDate, Date endDate) {
        appDatabase.categoryBudgetDao().deleteAllForMonth(userId, startDate, endDate);
    }

    @Override
    public CategoryBudgetEntity getCategoryBudgetForMonth(int userId, String category, Date startDate, Date endDate) {
        return appDatabase.categoryBudgetDao().getCategoryBudgetForMonth(userId, category, startDate, endDate);
    }

    @Override
    public void update(CategoryBudgetEntity categoryBudget) {
        appDatabase.categoryBudgetDao().update(categoryBudget);
    }

    @Override
    public void insert(CategoryBudgetEntity categoryBudget) {
        appDatabase.categoryBudgetDao().insert(categoryBudget);
    }
}