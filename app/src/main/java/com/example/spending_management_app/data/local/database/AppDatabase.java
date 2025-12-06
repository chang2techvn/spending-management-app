package com.example.spending_management_app.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.spending_management_app.data.local.converter.DateConverter;
import com.example.spending_management_app.data.local.dao.BudgetDao;
import com.example.spending_management_app.data.local.dao.BudgetHistoryDao;
import com.example.spending_management_app.data.local.dao.CategoryBudgetDao;
import com.example.spending_management_app.data.local.dao.RecurringExpenseDao;
import com.example.spending_management_app.data.local.dao.TransactionDao;
import com.example.spending_management_app.data.local.dao.UserDao;
import com.example.spending_management_app.data.local.entity.UserEntity;
import com.example.spending_management_app.data.local.entity.BudgetEntity;
import com.example.spending_management_app.data.local.entity.BudgetHistoryEntity;
import com.example.spending_management_app.data.local.entity.CategoryBudgetEntity;
import com.example.spending_management_app.data.local.entity.RecurringExpenseEntity;
import com.example.spending_management_app.data.local.entity.TransactionEntity;

@Database(entities = {TransactionEntity.class, BudgetEntity.class, RecurringExpenseEntity.class, CategoryBudgetEntity.class, BudgetHistoryEntity.class, UserEntity.class}, version = 6)
@TypeConverters(DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "spending_management_db")
                    .fallbackToDestructiveMigration() // Sẽ xóa và tạo lại DB khi schema thay đổi
                    .build();
        }
        return instance;
    }

    public abstract TransactionDao transactionDao();
    public abstract BudgetDao budgetDao();
    public abstract RecurringExpenseDao recurringExpenseDao();
    public abstract CategoryBudgetDao categoryBudgetDao();
    public abstract BudgetHistoryDao budgetHistoryDao();
    public abstract UserDao userDao();
}