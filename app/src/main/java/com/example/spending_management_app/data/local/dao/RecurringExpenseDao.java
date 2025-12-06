package com.example.spending_management_app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.spending_management_app.data.local.entity.RecurringExpenseEntity;

import java.util.List;

@Dao
public interface RecurringExpenseDao {
    @Insert
    void insert(RecurringExpenseEntity recurringExpense);

    @Query("SELECT * FROM recurring_expenses WHERE userId = :userId")
    List<RecurringExpenseEntity> getAllRecurringExpenses(int userId);
}