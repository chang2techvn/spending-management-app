package com.example.spending_management_app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RecurringExpenseDao {
    @Insert
    void insert(RecurringExpenseEntity recurringExpense);

    @Query("SELECT * FROM recurring_expenses")
    List<RecurringExpenseEntity> getAllRecurringExpenses();
}