package com.example.spending_management_app.data.repository;

import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.local.entity.BudgetEntity;
import com.example.spending_management_app.domain.repository.BudgetRepository;

import java.util.Date;
import java.util.List;

/**
 * Implementation of BudgetRepository
 */
public class BudgetRepositoryImpl implements BudgetRepository {

    private final AppDatabase appDatabase;

    public BudgetRepositoryImpl(AppDatabase appDatabase) {
        this.appDatabase = appDatabase;
    }

    @Override
    public List<BudgetEntity> getBudgetsByDateRange(int userId, Date startDate, Date endDate) {
        return appDatabase.budgetDao().getBudgetsByDateRange(userId, startDate, endDate);
    }

    @Override
    public List<BudgetEntity> getBudgetsByDateRangeOrdered(int userId, Date startDate, Date endDate) {
        return appDatabase.budgetDao().getBudgetsByDateRangeOrdered(userId, startDate, endDate);
    }

    @Override
    public void update(BudgetEntity budget) {
        appDatabase.budgetDao().update(budget);
    }

    @Override
    public void insert(BudgetEntity budget) {
        appDatabase.budgetDao().insert(budget);
    }

    @Override
    public void deleteBudgetsByDateRange(int userId, Date startDate, Date endDate) {
        appDatabase.budgetDao().deleteBudgetsByDateRange(userId, startDate, endDate);
    }
}