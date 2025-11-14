package com.example.spending_management_app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.spending_management_app.database.entity.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(TransactionEntity transaction);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    List<TransactionEntity> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE type = 'expense' ORDER BY date DESC")
    List<TransactionEntity> getAllExpenses();

    @Query("SELECT * FROM transactions WHERE type = 'income' ORDER BY date DESC")
    List<TransactionEntity> getAllIncomes();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'income'")
    Long getTotalIncome();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'expense'")
    Long getTotalExpense();
}