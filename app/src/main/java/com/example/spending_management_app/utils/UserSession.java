package com.example.spending_management_app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Quản lý session của user hiện tại
 * Lưu userId vào SharedPreferences để duy trì session
 */
public class UserSession {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    
    private static UserSession instance;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    
    private UserSession(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    
    public static synchronized UserSession getInstance(Context context) {
        if (instance == null) {
            instance = new UserSession(context);
        }
        return instance;
    }
    
    /**
     * Lưu thông tin đăng nhập của user
     */
    public void login(int userId) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }
    
    /**
     * Đăng xuất user hiện tại
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }
    
    /**
     * Lấy userId của user hiện tại
     * @return userId, hoặc 1 nếu chưa đăng nhập (default user)
     */
    public int getCurrentUserId() {
        return prefs.getInt(KEY_USER_ID, 1); // Default userId = 1
    }
    
    /**
     * Kiểm tra user đã đăng nhập chưa
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, true); // Default = true để dùng user mặc định
    }
    
    /**
     * Cập nhật userId
     */
    public void setCurrentUserId(int userId) {
        editor.putInt(KEY_USER_ID, userId);
        editor.apply();
    }
}
