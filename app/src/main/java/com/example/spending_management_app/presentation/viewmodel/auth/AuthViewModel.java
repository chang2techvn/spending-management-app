package com.example.spending_management_app.presentation.viewmodel.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.spending_management_app.data.local.entity.UserEntity;
import com.example.spending_management_app.domain.usecase.user.UserUseCase;

/**
 * ViewModel for authentication operations
 */
public class AuthViewModel extends ViewModel {

    private final UserUseCase userUseCase;

    // Login LiveData
    private final MutableLiveData<LoginState> loginState = new MutableLiveData<>();
    private final MutableLiveData<UserEntity> currentUser = new MutableLiveData<>();

    // Register LiveData
    private final MutableLiveData<RegisterState> registerState = new MutableLiveData<>();

    public AuthViewModel(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    /**
     * Login user
     */
    public void login(String emailOrPhone, String password) {
        loginState.setValue(LoginState.LOADING);

        // Run in background thread (in real app, use coroutine or executor)
        new Thread(() -> {
            UserUseCase.LoginResult result = userUseCase.loginUser(emailOrPhone, password);
            if (result.success) {
                currentUser.postValue(result.user);
                loginState.postValue(LoginState.SUCCESS);
            } else {
                loginState.postValue(new LoginState.Error(result.message));
            }
        }).start();
    }

    /**
     * Register user
     */
    public void register(String emailOrPhone, String password, String confirmPassword) {
        registerState.setValue(RegisterState.LOADING);

        // Run in background thread
        new Thread(() -> {
            UserUseCase.RegisterResult result = userUseCase.registerUser(emailOrPhone, password, confirmPassword);
            if (result.success) {
                currentUser.postValue(result.user);
                registerState.postValue(RegisterState.SUCCESS);
            } else {
                registerState.postValue(new RegisterState.Error(result.message));
            }
        }).start();
    }

    /**
     * Set current user (for session management)
     */
    public void setCurrentUser(UserEntity user) {
        currentUser.setValue(user);
    }

    // LiveData getters
    public LiveData<LoginState> getLoginState() {
        return loginState;
    }

    public LiveData<RegisterState> getRegisterState() {
        return registerState;
    }

    public LiveData<UserEntity> getCurrentUser() {
        return currentUser;
    }

    // State classes
    public static class LoginState {
        public static final LoginState LOADING = new LoginState();
        public static final LoginState SUCCESS = new LoginState();

        public static class Error extends LoginState {
            public final String message;
            public Error(String message) {
                this.message = message;
            }
        }
    }

    public static class RegisterState {
        public static final RegisterState LOADING = new RegisterState();
        public static final RegisterState SUCCESS = new RegisterState();

        public static class Error extends RegisterState {
            public final String message;
            public Error(String message) {
                this.message = message;
            }
        }
    }
}