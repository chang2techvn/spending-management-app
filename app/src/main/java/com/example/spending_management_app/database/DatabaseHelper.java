package com.example.spending_management_app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.spending_management_app.utils.PasswordHasher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "spending.db";
    private static final int DATABASE_VERSION = 1; // Incremented version

    private static DatabaseHelper instance;

    // Table Names
    public static final String TABLE_USER = "User";
    public static final String TABLE_CATEGORY = "Category";
    public static final String TABLE_EXPENSE = "Expense";
    public static final String TABLE_BUDGET = "Budget";
    public static final String TABLE_ALERT = "Alert";
    public static final String TABLE_HISTORY = "History";
    public static final String TABLE_STATISTICS = "Statistics";
    public static final String TABLE_NOTIFICATION = "Notification";
    public static final String TABLE_SETTING = "Setting";
    public static final String TABLE_SUPPORT_REQUEST = "SupportRequest";
    public static final String TABLE_AI_ASSISTANT = "AIAssistant";

    // User Table Columns
    public static final String KEY_USER_ID = "UserID";
    public static final String KEY_USER_FIRST_NAME = "FirstName";
    public static final String KEY_USER_LAST_NAME = "LastName";
    public static final String KEY_USER_EMAIL = "Email";
    public static final String KEY_USER_PASSWORD = "Password";
    public static final String KEY_USER_JOIN_DATE = "JoinDate";

    // Common Columns for new tables
    public static final String KEY_CATEGORY_ID = "CategoryID";
    public static final String KEY_EXPENSE_ID = "ExpenseID";
    public static final String KEY_BUDGET_ID = "BudgetID";
    public static final String KEY_ALERT_ID = "AlertID";


    // --- TABLE CREATE STATEMENTS ---

    private static final String CREATE_TABLE_USER = "CREATE TABLE " + TABLE_USER + "("
            + KEY_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USER_FIRST_NAME + " TEXT NOT NULL,"
            + KEY_USER_LAST_NAME + " TEXT NOT NULL,"
            + KEY_USER_EMAIL + " TEXT UNIQUE NOT NULL,"
            + KEY_USER_PASSWORD + " TEXT NOT NULL,"
            + KEY_USER_JOIN_DATE + " TEXT NOT NULL" + ")";

    private static final String CREATE_TABLE_CATEGORY = "CREATE TABLE " + TABLE_CATEGORY + "("
            + KEY_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "Name TEXT, Type TEXT)";

    private static final String CREATE_TABLE_EXPENSE = "CREATE TABLE " + TABLE_EXPENSE + "("
            + KEY_EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USER_ID + " INTEGER, " + KEY_CATEGORY_ID + " INTEGER,"
            + "Amount REAL, Description TEXT, Date TEXT,"
            + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USER + "(" + KEY_USER_ID + "),"
            + "FOREIGN KEY(" + KEY_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORY + "(" + KEY_CATEGORY_ID + "))";

    private static final String CREATE_TABLE_BUDGET = "CREATE TABLE " + TABLE_BUDGET + "("
            + KEY_BUDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USER_ID + " INTEGER, " + KEY_CATEGORY_ID + " INTEGER,"
            + "AmountLimit REAL, Type TEXT, StartDate TEXT, EndDate TEXT,"
            + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USER + "(" + KEY_USER_ID + "),"
            + "FOREIGN KEY(" + KEY_CATEGORY_ID + ") REFERENCES " + TABLE_CATEGORY + "(" + KEY_CATEGORY_ID + "))";

    private static final String CREATE_TABLE_ALERT = "CREATE TABLE " + TABLE_ALERT + "("
            + KEY_ALERT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USER_ID + " INTEGER, " + KEY_EXPENSE_ID + " INTEGER, " + KEY_BUDGET_ID + " INTEGER,"
            + "AlertDate TEXT, AlertType TEXT, Message TEXT,"
            + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USER + "(" + KEY_USER_ID + "))";

    private static final String CREATE_TABLE_HISTORY = "CREATE TABLE " + TABLE_HISTORY + "("
            + "HistoryID INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USER_ID + " INTEGER, " + KEY_BUDGET_ID + " INTEGER, " + KEY_EXPENSE_ID + " INTEGER,"
            + "ActionType TEXT, OldValue TEXT, NewValue TEXT, ActionDate TEXT,"
            + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USER + "(" + KEY_USER_ID + "))";

    private static final String CREATE_TABLE_STATISTICS = "CREATE TABLE " + TABLE_STATISTICS + "("
            + "StatID INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USER_ID + " INTEGER,"
            + "PeriodType TEXT, TotalExpense REAL, TotalBudget REAL, Remaining REAL, CategoryExpenses TEXT,"
            + "BudgetAlertsCount INTEGER, AvgExpense REAL, MaxExpense REAL, MinExpense REAL,"
            + "CreatedAt TEXT, UpdatedAt TEXT,"
            + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USER + "(" + KEY_USER_ID + "))";

    private static final String CREATE_TABLE_NOTIFICATION = "CREATE TABLE " + TABLE_NOTIFICATION + "("
            + "NotificationID INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USER_ID + " INTEGER, " + KEY_ALERT_ID + " INTEGER,"
            + "Message TEXT, Date TEXT, Status TEXT,"
            + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USER + "(" + KEY_USER_ID + "))";

    private static final String CREATE_TABLE_SETTING = "CREATE TABLE " + TABLE_SETTING + "("
            + "SettingID INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USER_ID + " INTEGER,"
            + "Currency TEXT, Language TEXT, Theme TEXT, NotificationPreference TEXT,"
            + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USER + "(" + KEY_USER_ID + "))";

    private static final String CREATE_TABLE_SUPPORT_REQUEST = "CREATE TABLE " + TABLE_SUPPORT_REQUEST + "("
            + "SupportID INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USER_ID + " INTEGER,"
            + "Title TEXT, Description TEXT, Status TEXT, CreatedAt TEXT, ResolvedAt TEXT,"
            + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USER + "(" + KEY_USER_ID + "))";

    private static final String CREATE_TABLE_AI_ASSISTANT = "CREATE TABLE " + TABLE_AI_ASSISTANT + "("
            + "AIID INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "ModelName TEXT, Version TEXT, Function TEXT)";


    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USER);
        db.execSQL(CREATE_TABLE_CATEGORY);
        db.execSQL(CREATE_TABLE_EXPENSE);
        db.execSQL(CREATE_TABLE_BUDGET);
        db.execSQL(CREATE_TABLE_ALERT);
        db.execSQL(CREATE_TABLE_HISTORY);
        db.execSQL(CREATE_TABLE_STATISTICS);
        db.execSQL(CREATE_TABLE_NOTIFICATION);
        db.execSQL(CREATE_TABLE_SETTING);
        db.execSQL(CREATE_TABLE_SUPPORT_REQUEST);
        db.execSQL(CREATE_TABLE_AI_ASSISTANT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AI_ASSISTANT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUPPORT_REQUEST);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTING);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATISTICS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALERT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGET);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);

        onCreate(db);
    }

    public long addUser(String firstName, String lastName, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USER_FIRST_NAME, firstName);
        values.put(KEY_USER_LAST_NAME, lastName);
        values.put(KEY_USER_EMAIL, email);
        values.put(KEY_USER_PASSWORD, PasswordHasher.hashPassword(password));
        values.put(KEY_USER_JOIN_DATE, getCurrentDateTime());

        long id = db.insert(TABLE_USER, null, values);
        return id;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String hashedPassword = PasswordHasher.hashPassword(password);

        String[] columns = {KEY_USER_ID};
        String selection = KEY_USER_EMAIL + " = ? AND " + KEY_USER_PASSWORD + " = ?";
        String[] selectionArgs = {email, hashedPassword};

        Cursor cursor = db.query(TABLE_USER, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();

        return count > 0;
    }

    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        return dateFormat.format(new Date());
    }
}
