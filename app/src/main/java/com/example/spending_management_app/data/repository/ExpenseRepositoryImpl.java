package com.example.spending_management_app.data.repository;

import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.local.entity.TransactionEntity;
import com.example.spending_management_app.domain.repository.ExpenseRepository;

import java.util.Calendar;
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
    public void update(TransactionEntity transaction) {
        appDatabase.transactionDao().update(transaction);
    }

    @Override
    public void delete(TransactionEntity transaction) {
        appDatabase.transactionDao().delete(transaction);
    }

    @Override
    public TransactionEntity getTransactionById(int userId, int id) {
        return appDatabase.transactionDao().getTransactionById(userId, id);
    }

    @Override
    public List<TransactionEntity> getTransactionsByDateRange(int userId, Date startDate, Date endDate) {
        return appDatabase.transactionDao().getTransactionsByDateRange(userId, startDate, endDate);
    }

    @Override
    public List<TransactionEntity> getRecentTransactions(int userId, int limit) {
        return appDatabase.transactionDao().getRecentTransactions(userId, limit);
    }

    @Override
    public List<TransactionEntity> getTransactionsByDate(int userId, Date date) {
        // Create date range for the same day (from start of day to end of day)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calendar.getTime();

        return appDatabase.transactionDao().getTransactionsByDateRange(userId, startOfDay, endOfDay);
    }

    @Override
    public List<TransactionEntity> getAllTransactions(int userId) {
        return appDatabase.transactionDao().getAllTransactions(userId);
    }
}