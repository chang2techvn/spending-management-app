package com.example.spending_management_app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.spending_management_app.data.local.entity.UserEntity;
import com.google.gson.Gson;

/**
 * Session manager for user authentication state
 * Uses SharedPreferences to persist user session
 */
public class SessionManager {

    private static final String PREF_NAME = "user_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_DATA = "user_data";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;
    private Gson gson;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        gson = new Gson();
    }

    /**
     * Create login session
     */
    public void createLoginSession(UserEntity user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_DATA, gson.toJson(user));
        editor.commit();
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get logged in user data
     */
    public UserEntity getUserData() {
        String userJson = pref.getString(KEY_USER_DATA, null);
        if (userJson != null) {
            return gson.fromJson(userJson, UserEntity.class);
        }
        return null;
    }

    /**
     * Logout user and clear session
     */
    public void logout() {
        editor.clear();
        editor.commit();
    }

    /**
     * Update user data in session
     */
    public void updateUserData(UserEntity user) {
        if (isLoggedIn()) {
            editor.putString(KEY_USER_DATA, gson.toJson(user));
            editor.commit();
        }
    }
}