package com.example.spending_management_app.data.repository;

import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.local.entity.TransactionEntity;
import com.example.spending_management_app.domain.repository.ExpenseRepository;

import java.util.Date;
import java.util.List;

/**
 * Implementation of ExpenseRepository
 */
public class ExpenseRepositoryImpl implements ExpenseRepository {

    private final AppDatabase appDatabase;

    public ExpenseRepositoryImpl(AppDatabase appDatabase) {
        this.appDatabase = appDatabase;
    }

    @Override
    public void insert(TransactionEntity transaction) {
        appDatabase.transactionDao().insert(transaction);
    }

    @Override
    public void delete(TransactionEntity transaction) {
        appDatabase.transactionDao().delete(transaction);
    }

    @Override
    public TransactionEntity getTransactionById(int id) {
        return appDatabase.transactionDao().getTransactionById(id);
    }

    @Override
    public List<TransactionEntity> getTransactionsByDateRange(Date startDate, Date endDate) {
        return appDatabase.transactionDao().getTransactionsByDateRange(startDate, endDate);
    }

    @Override
    public List<TransactionEntity> getRecentTransactions(int limit) {
        return appDatabase.transactionDao().getRecentTransactions(limit);
    }
}