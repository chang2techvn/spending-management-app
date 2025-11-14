package com.example.spending_management_app.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.spending_management_app.MainActivity;
import com.example.spending_management_app.R;
import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.entity.UserEntity;
import com.example.spending_management_app.ui.register.RegisterActivity;
import com.example.spending_management_app.utils.PasswordHasher;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private View emailStatusLine, passwordStatusLine;
    private Button buttonLogin;
    private TextView textViewRegister;
    private ImageView passwordVisibilityToggle;
    private AppDatabase appDatabase;
    public static final String KEY_USER_ID = "current_user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        appDatabase = AppDatabase.getInstance(getApplicationContext());

        initViews();
        setupListeners();
    }

    private void initViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        emailStatusLine = findViewById(R.id.emailStatusLine);
        passwordStatusLine = findViewById(R.id.passwordStatusLine);
        buttonLogin = findViewById(R.id.signinButton);
        textViewRegister = findViewById(R.id.signupText);
        passwordVisibilityToggle = findViewById(R.id.passwordVisibilityToggle);
    }

    private void setupListeners() {
        addTextWatcher(editTextEmail, emailStatusLine);
        addTextWatcher(editTextPassword, passwordStatusLine);
        setupPasswordVisibilityToggle(passwordVisibilityToggle, editTextPassword);

        buttonLogin.setOnClickListener(v -> loginUser());

        textViewRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void addTextWatcher(EditText editText, View underline) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    underline.setBackgroundColor(Color.parseColor("#BDBDBD"));
                } else {
                    underline.setBackgroundColor(ContextCompat.getColor(LoginActivity.this, R.color.primaryBlue));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupPasswordVisibilityToggle(ImageView toggle, EditText editText) {
        toggle.setOnClickListener(v -> {
            android.graphics.Typeface typeface = editText.getTypeface();
            int selection = editText.getSelectionEnd();
            if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggle.setImageResource(R.drawable.ic_view);
            } else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggle.setImageResource(R.drawable.ic_view_off);
            }
            editText.setTypeface(typeface);
            editText.setSelection(selection);
        });
    }

    private void loginUser() {
        resetUnderlines();

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            if (TextUtils.isEmpty(email)) {
                emailStatusLine.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed));
            }
            if (TextUtils.isEmpty(password)) {
                passwordStatusLine.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed));
            }
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String hashedPassword = PasswordHasher.hashPassword(password);

        Executors.newSingleThreadExecutor().execute(() -> {
            UserEntity user = appDatabase.userDao().findByUser(email, hashedPassword);
            runOnUiThread(() -> {
                if (user != null) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    prefs.edit().putInt(KEY_USER_ID, user.userId).apply();

                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    emailStatusLine.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed));
                    passwordStatusLine.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed));
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void resetUnderlines() {
        int defaultColor = Color.TRANSPARENT;
        int primaryColor = ContextCompat.getColor(this, R.color.primaryBlue);

        emailStatusLine.setBackgroundColor(TextUtils.isEmpty(editTextEmail.getText()) ? defaultColor : primaryColor);
        passwordStatusLine.setBackgroundColor(TextUtils.isEmpty(editTextPassword.getText()) ? defaultColor : primaryColor);
    }
}
