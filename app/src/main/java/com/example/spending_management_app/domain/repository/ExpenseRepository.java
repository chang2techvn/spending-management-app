package com.example.spending_management_app.domain.repository;

import com.example.spending_management_app.data.local.entity.TransactionEntity;

import java.util.Date;
import java.util.List;

/**
 * Repository interface for expense operations
 */
public interface ExpenseRepository {
    void insert(TransactionEntity transaction);
    void update(TransactionEntity transaction);
    void delete(TransactionEntity transaction);
    TransactionEntity getTransactionById(int id);
    List<TransactionEntity> getTransactionsByDateRange(Date startDate, Date endDate);
    List<TransactionEntity> getTransactionsByDate(Date date);
    List<TransactionEntity> getRecentTransactions(int limit);
    List<TransactionEntity> getAllTransactions();
}