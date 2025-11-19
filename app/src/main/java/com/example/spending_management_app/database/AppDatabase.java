package com.example.spending_management_app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {TransactionEntity.class, BudgetEntity.class, RecurringExpenseEntity.class, CategoryBudgetEntity.class}, version = 3)
@TypeConverters(DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "spending_management_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract TransactionDao transactionDao();
    public abstract BudgetDao budgetDao();
    public abstract RecurringExpenseDao recurringExpenseDao();
    public abstract CategoryBudgetDao categoryBudgetDao();
}