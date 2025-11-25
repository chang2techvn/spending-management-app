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
import com.example.spending_management_app.databinding.ActivitySignupBinding;
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

    private ActivitySignupBinding binding;
    private AuthViewModel authViewModel;
    private SessionManager sessionManager;

    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button signupButton;
    private ProgressBar progressBar;
    private ImageView passwordVisibilityToggle;
    private ImageView confirmPasswordVisibilityToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        binding = ActivitySignupBinding.inflate(getLayoutInflater());
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
        confirmPasswordInput = binding.editTextConfirmPassword;
        signupButton = binding.signupButton;
        progressBar = binding.progressBar;
        passwordVisibilityToggle = binding.passwordVisibilityToggle;
        confirmPasswordVisibilityToggle = binding.confirmPasswordVisibilityToggle;
    }

    private void setupListeners() {
        signupButton.setOnClickListener(v -> performRegister());

        binding.signinText.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        passwordVisibilityToggle.setOnClickListener(v -> togglePasswordVisibility());
        confirmPasswordVisibilityToggle.setOnClickListener(v -> toggleConfirmPasswordVisibility());
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

        // Basic validation
        boolean isValid = true;

        if (emailOrPhone.isEmpty()) {
            emailInput.setError(getString(R.string.email_or_phone_required));
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordInput.setError(getString(R.string.password_required));
            isValid = false;
        } else if (password.length() < 6) {
            passwordInput.setError(getString(R.string.password_too_short));
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.setError(getString(R.string.confirm_password_required));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.passwords_not_match));
            isValid = false;
        }

        if (isValid) {
            authViewModel.register(emailOrPhone, password, confirmPassword, this);
        }
    }

    private void handleRegisterSuccess() {
        // Get user data and save to session
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                sessionManager.createLoginSession(user);
                Toast.makeText(this, getString(R.string.registration_success_with_name, user.getName()), Toast.LENGTH_LONG).show();
                navigateToMain();
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        signupButton.setEnabled(false);
        signupButton.setText(getString(R.string.registering));
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        signupButton.setEnabled(true);
        signupButton.setText("Đăng ký");
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

    private void toggleConfirmPasswordVisibility() {
        if (confirmPasswordInput.getTransformationMethod() instanceof PasswordTransformationMethod) {
            // Show password
            confirmPasswordInput.setTransformationMethod(null);
            confirmPasswordVisibilityToggle.setImageResource(R.drawable.ic_view_off);
        } else {
            // Hide password
            confirmPasswordInput.setTransformationMethod(new PasswordTransformationMethod());
            confirmPasswordVisibilityToggle.setImageResource(R.drawable.ic_view);
        }
        confirmPasswordInput.setSelection(confirmPasswordInput.getText().length());
    }
}