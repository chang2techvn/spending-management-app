package com.example.spending_management_app.ui.register;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

import com.example.spending_management_app.R;
import com.example.spending_management_app.database.AppDatabase;
import com.example.spending_management_app.database.entity.UserEntity;
import com.example.spending_management_app.ui.login.LoginActivity;
import com.example.spending_management_app.utils.PasswordHasher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextFirstName, editTextLastName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private View firstNameStatusLine, lastNameStatusLine, emailStatusLine, passwordStatusLine, confirmPasswordStatusLine;
    private Button buttonRegister;
    private TextView textViewLogin;
    private ImageView passwordVisibilityToggle, confirmPasswordVisibilityToggle;
    private AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        appDatabase = AppDatabase.getInstance(getApplicationContext());

        initViews();
        setupListeners();
    }

    private void initViews() {
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.signupButton);
        textViewLogin = findViewById(R.id.signinText);

        firstNameStatusLine = findViewById(R.id.firstNameStatusLine);
        lastNameStatusLine = findViewById(R.id.lastNameStatusLine);
        emailStatusLine = findViewById(R.id.emailStatusLine);
        passwordStatusLine = findViewById(R.id.passwordStatusLine);
        confirmPasswordStatusLine = findViewById(R.id.confirmPasswordStatusLine);

        passwordVisibilityToggle = findViewById(R.id.passwordVisibilityToggle);
        confirmPasswordVisibilityToggle = findViewById(R.id.confirmPasswordVisibilityToggle);
    }

    private void setupListeners() {
        addTextWatcher(editTextFirstName, firstNameStatusLine);
        addTextWatcher(editTextLastName, lastNameStatusLine);
        addTextWatcher(editTextEmail, emailStatusLine);
        addTextWatcher(editTextPassword, passwordStatusLine);
        addTextWatcher(editTextConfirmPassword, confirmPasswordStatusLine);

        setupPasswordVisibilityToggle(passwordVisibilityToggle, editTextPassword);
        setupPasswordVisibilityToggle(confirmPasswordVisibilityToggle, editTextConfirmPassword);

        buttonRegister.setOnClickListener(v -> registerUser());
        textViewLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
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
                    underline.setBackgroundColor(ContextCompat.getColor(RegisterActivity.this, R.color.primaryBlue));
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

    private void registerUser() {
        resetUnderlines();

        String firstName = editTextFirstName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        boolean hasError = false;

        if (TextUtils.isEmpty(firstName)) {
            firstNameStatusLine.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed));
            hasError = true;
        }
        if (TextUtils.isEmpty(lastName)) {
            lastNameStatusLine.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed));
            hasError = true;
        }
        if (TextUtils.isEmpty(email)) {
            emailStatusLine.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed));
            hasError = true;
        }
        if (TextUtils.isEmpty(password)) {
            passwordStatusLine.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed));
            hasError = true;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordStatusLine.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed));
            hasError = true;
        }

        if (hasError) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            passwordStatusLine.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed));
            confirmPasswordStatusLine.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed));
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            UserEntity existingUser = appDatabase.userDao().findUserByEmail(email);
            if (existingUser != null) {
                runOnUiThread(() -> {
                    emailStatusLine.setBackgroundColor(ContextCompat.getColor(this, R.color.errorRed));
                    Toast.makeText(RegisterActivity.this, "Email already exists.", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            UserEntity newUser = new UserEntity();
            newUser.firstName = firstName;
            newUser.lastName = lastName;
            newUser.email = email;
            newUser.password = PasswordHasher.hashPassword(password);
            newUser.joinDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

            long newRowId = appDatabase.userDao().insert(newUser);

            runOnUiThread(() -> {
                if (newRowId != -1) {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Registration failed.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void resetUnderlines() {
        int defaultColor = Color.TRANSPARENT;
        int primaryColor = ContextCompat.getColor(this, R.color.primaryBlue);

        firstNameStatusLine.setBackgroundColor(TextUtils.isEmpty(editTextFirstName.getText()) ? defaultColor : primaryColor);
        lastNameStatusLine.setBackgroundColor(TextUtils.isEmpty(editTextLastName.getText()) ? defaultColor : primaryColor);
        emailStatusLine.setBackgroundColor(TextUtils.isEmpty(editTextEmail.getText()) ? defaultColor : primaryColor);
        passwordStatusLine.setBackgroundColor(TextUtils.isEmpty(editTextPassword.getText()) ? defaultColor : primaryColor);
        confirmPasswordStatusLine.setBackgroundColor(TextUtils.isEmpty(editTextConfirmPassword.getText()) ? defaultColor : primaryColor);
    }
}
