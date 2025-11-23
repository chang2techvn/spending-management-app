package com.example.spending_management_app;

import android.app.Application;
import android.content.Context;

import com.example.spending_management_app.utils.LocaleHelper;

/**
 * Application class to initialize app-wide settings
 */
public class SpendingManagementApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Set the app's locale based on saved preference
        String language = LocaleHelper.getLanguage(this);
        LocaleHelper.setLocale(this, language);
    }

    @Override
    protected void attachBaseContext(Context base) {
        // Set locale before super.attachBaseContext
        String language = LocaleHelper.getLanguage(base);
        Context context = LocaleHelper.updateContextLocale(base, language);
        super.attachBaseContext(context);
    }
}