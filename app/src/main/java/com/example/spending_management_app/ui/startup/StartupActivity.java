package com.example.spending_management_app.ui.startup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spending_management_app.R;
import com.example.spending_management_app.ui.login.LoginActivity;

public class StartupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        ImageButton continueButton = findViewById(R.id.continueButton);

        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(StartupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
