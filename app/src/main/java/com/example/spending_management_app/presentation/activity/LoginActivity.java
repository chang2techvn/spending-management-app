package com.example.spending_management_app.presentation.activity;

import android.content.Intent;
import android.text.method.PasswordTransformationMethod;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.spending_management_app.R;
import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.repository.UserRepositoryImpl;
import com.example.spending_management_app.databinding.ActivitySigninBinding;
import com.example.spending_management_app.domain.repository.UserRepository;
import com.example.spending_management_app.domain.usecase.user.UserUseCase;
import com.example.spending_management_app.presentation.viewmodel.auth.AuthViewModel;
import com.example.spending_management_app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Login Activity with modern UI
 */
public class LoginActivity extends AppCompatActivity {

    private ActivitySigninBinding binding;
    private AuthViewModel authViewModel;
    private SessionManager sessionManager;

    private EditText emailInput;
    private EditText passwordInput;
    private Button signinButton;
    private ProgressBar progressBar;
    private ImageView passwordVisibilityToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        binding = ActivitySigninBinding.inflate(getLayoutInflater());
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
        emailInput = binding.editTextEmail;
        passwordInput = binding.editTextPassword;
        signinButton = binding.signinButton;
        progressBar = binding.progressBar;
        passwordVisibilityToggle = binding.passwordVisibilityToggle;
    }

    private void setupListeners() {
        signinButton.setOnClickListener(v -> performLogin());

        binding.signupText.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });

        passwordVisibilityToggle.setOnClickListener(v -> togglePasswordVisibility());
    }

    private void observeViewModel() {
        authViewModel.getLoginState().observe(this, loginState -> {
            if (loginState instanceof AuthViewModel.LoginState.Error) {
                AuthViewModel.LoginState.Error errorState = (AuthViewModel.LoginState.Error) loginState;
                showError(errorState.message);
                hideLoading();
            } else if (loginState == AuthViewModel.LoginState.LOADING) {
                showLoading();
            } else if (loginState == AuthViewModel.LoginState.SUCCESS) {
                hideLoading();
                handleLoginSuccess();
            }
        });
    }

    private void performLogin() {
        String emailOrPhone = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Basic validation
        boolean isValid = true;

        if (emailOrPhone.isEmpty()) {
            emailInput.setError("Vui lòng nhập email hoặc số điện thoại");
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        }

        if (isValid) {
            authViewModel.login(emailOrPhone, password);
        }
    }

    private void handleLoginSuccess() {
        // Get user data and save to session
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                sessionManager.createLoginSession(user);
                navigateToMain();
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        signinButton.setEnabled(false);
        signinButton.setText("Đang đăng nhập...");
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        signinButton.setEnabled(true);
        signinButton.setText("Đăng nhập");
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

    private void togglePasswordVisibility() {
        if (passwordInput.getTransformationMethod() instanceof PasswordTransformationMethod) {
            // Show password
            passwordInput.setTransformationMethod(null);
            passwordVisibilityToggle.setImageResource(R.drawable.ic_view_off);
        } else {
            // Hide password
            passwordInput.setTransformationMethod(new PasswordTransformationMethod());
            passwordVisibilityToggle.setImageResource(R.drawable.ic_view);
        }
        passwordInput.setSelection(passwordInput.getText().length());
    }
}