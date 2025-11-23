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
    public List<BudgetEntity> getBudgetsByDateRange(Date startDate, Date endDate) {
        return appDatabase.budgetDao().getBudgetsByDateRange(startDate, endDate);
    }

    @Override
    public List<BudgetEntity> getBudgetsByDateRangeOrdered(Date startDate, Date endDate) {
        return appDatabase.budgetDao().getBudgetsByDateRangeOrdered(startDate, endDate);
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
    public void deleteBudgetsByDateRange(Date startDate, Date endDate) {
        appDatabase.budgetDao().deleteBudgetsByDateRange(startDate, endDate);
    }
}