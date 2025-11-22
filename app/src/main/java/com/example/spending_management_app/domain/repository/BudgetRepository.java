package com.example.spending_management_app.domain.repository;

import com.example.spending_management_app.data.local.entity.BudgetEntity;

import java.util.Date;
import java.util.List;

/**
 * Repository interface for budget operations
 */
public interface BudgetRepository {
    List<BudgetEntity> getBudgetsByDateRange(Date startDate, Date endDate);
    List<BudgetEntity> getBudgetsByDateRangeOrdered(Date startDate, Date endDate);
    void update(BudgetEntity budget);
    void insert(BudgetEntity budget);
    void deleteBudgetsByDateRange(Date startDate, Date endDate);
}