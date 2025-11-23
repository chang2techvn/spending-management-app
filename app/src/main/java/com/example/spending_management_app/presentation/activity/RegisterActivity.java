package com.example.spending_management_app.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.spending_management_app.R;
import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.repository.UserRepositoryImpl;
import com.example.spending_management_app.databinding.ActivityRegisterBinding;
import com.example.spending_management_app.domain.repository.UserRepository;
import com.example.spending_management_app.domain.usecase.user.UserUseCase;
import com.example.spending_management_app.presentation.viewmodel.auth.AuthViewModel;
import com.example.spending_management_app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Register Activity with modern UI and validation
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel authViewModel;
    private SessionManager sessionManager;

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton registerButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeDependencies();
        initializeViews();
        setupListeners();
        observeViewModel();
    }

    private void initializeDependencies() {
        // Initialize database and repositories
        AppDatabase appDatabase = AppDatabase.getInstance(this);
        UserRepository userRepository = new UserRepositoryImpl(appDatabase);
        UserUseCase userUseCase = new UserUseCase(userRepository);

        // Initialize ViewModel
        authViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                return (T) new AuthViewModel(userUseCase);
            }
        }).get(AuthViewModel.class);
    }

    private void initializeViews() {
        emailLayout = binding.emailLayout;
        passwordLayout = binding.passwordLayout;
        confirmPasswordLayout = binding.confirmPasswordLayout;
        emailInput = binding.emailInput;
        passwordInput = binding.passwordInput;
        confirmPasswordInput = binding.confirmPasswordInput;
        registerButton = binding.registerButton;
        progressBar = binding.progressBar;
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> performRegister());

        binding.loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void observeViewModel() {
        authViewModel.getRegisterState().observe(this, registerState -> {
            if (registerState instanceof AuthViewModel.RegisterState.Error) {
                AuthViewModel.RegisterState.Error errorState = (AuthViewModel.RegisterState.Error) registerState;
                showError(errorState.message);
                hideLoading();
            } else if (registerState == AuthViewModel.RegisterState.LOADING) {
                showLoading();
            } else if (registerState == AuthViewModel.RegisterState.SUCCESS) {
                hideLoading();
                handleRegisterSuccess();
            }
        });
    }

    private void performRegister() {
        String emailOrPhone = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Clear previous errors
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);

        // Basic validation
        boolean isValid = true;

        if (emailOrPhone.isEmpty()) {
            emailLayout.setError("Vui lòng nhập email hoặc số điện thoại");
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Mật khẩu phải có ít nhất 6 ký tự");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError("Vui lòng xác nhận mật khẩu");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Mật khẩu xác nhận không khớp");
            isValid = false;
        }

        if (isValid) {
            authViewModel.register(emailOrPhone, password, confirmPassword);
        }
    }

    private void handleRegisterSuccess() {
        // Get user data and save to session
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                sessionManager.createLoginSession(user);
                Toast.makeText(this, "Đăng ký thành công! Chào mừng " + user.getName(), Toast.LENGTH_LONG).show();
                navigateToMain();
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);
        registerButton.setText("Đang đăng ký...");
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        registerButton.setEnabled(true);
        registerButton.setText("Đăng ký");
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}