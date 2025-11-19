package com.example.spending_management_app.ui.startup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spending_management_app.MainActivity;
import com.example.spending_management_app.R;
import com.example.spending_management_app.ui.login.LoginActivity;
import com.example.spending_management_app.ui.register.RegisterActivity;

public class StartupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isFirstRun = prefs.getBoolean("is_first_run", true);
        boolean rememberMe = prefs.getBoolean(LoginActivity.KEY_REMEMBER, false);
        int userId = prefs.getInt(LoginActivity.KEY_USER_ID, -1);

        if (!isFirstRun) {
            if (rememberMe && userId != -1) {
                startActivity(new Intent(this, MainActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
            return;
        }

        setContentView(R.layout.activity_startup);

        ImageButton continueButton = findViewById(R.id.continueButton);

        continueButton.setOnClickListener(v -> {
            prefs.edit().putBoolean("is_first_run", false).apply();
            Intent intent = new Intent(StartupActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
