package com.example.spending_management_app.domain.usecase.user;

import android.content.Context;

import com.example.spending_management_app.R;
import com.example.spending_management_app.data.local.entity.UserEntity;
import com.example.spending_management_app.domain.repository.UserRepository;
import com.example.spending_management_app.utils.PasswordUtils;

/**
 * Use case for user authentication operations
 */
public class UserUseCase {

    private final UserRepository userRepository;

    public UserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Register new user
     * @param emailOrPhone Email or phone number
     * @param password Plain text password
     * @param confirmPassword Password confirmation
     * @param context Android context for string resources
     * @return Result with success status and user data
     */
    public RegisterResult registerUser(String emailOrPhone, String password, String confirmPassword, Context context) {
        // Validate input
        if (emailOrPhone == null || emailOrPhone.trim().isEmpty()) {
            return new RegisterResult(false, "Email hoặc số điện thoại không được để trống");
        }

        if (password == null || password.length() < 6) {
            return new RegisterResult(false, "Mật khẩu phải có ít nhất 6 ký tự");
        }

        if (!password.equals(confirmPassword)) {
            return new RegisterResult(false, "Mật khẩu xác nhận không khớp");
        }

        // Check if user already exists
        if (userRepository.isUserExists(emailOrPhone.trim())) {
            return new RegisterResult(false, "Email hoặc số điện thoại đã được đăng ký");
        }

        // Generate user data
        String hashedPassword = PasswordUtils.hashPassword(password);
        String name;
        String avatar;

        if (isEmail(emailOrPhone)) {
            // Email registration
            name = PasswordUtils.extractNameFromEmail(emailOrPhone.trim());
            avatar = PasswordUtils.generateDefaultAvatar(name);
        } else {
            // Phone registration
            name = PasswordUtils.generateRandomUsername();
            avatar = PasswordUtils.generateDefaultAvatar(name);
        }

        // Create user entity
        UserEntity user = new UserEntity(emailOrPhone.trim(), hashedPassword, name, avatar);

        // Save to database
        long userId = userRepository.registerUser(user);
        if (userId > 0) {
            user.setId((int) userId);
            return new RegisterResult(true, context.getString(R.string.registration_success), user);
        } else {
            return new RegisterResult(false, context.getString(R.string.registration_failed));
        }
    }

    /**
     * Login user
     * @param emailOrPhone Email or phone number
     * @param password Plain text password
     * @return Result with success status and user data
     */
    public LoginResult loginUser(String emailOrPhone, String password) {
        if (emailOrPhone == null || emailOrPhone.trim().isEmpty()) {
            return new LoginResult(false, "Email hoặc số điện thoại không được để trống");
        }

        if (password == null || password.isEmpty()) {
            return new LoginResult(false, "Mật khẩu không được để trống");
        }

        UserEntity user = userRepository.loginUser(emailOrPhone.trim(), password);
        if (user != null) {
            return new LoginResult(true, "Đăng nhập thành công", user);
        } else {
            return new LoginResult(false, "Email/số điện thoại hoặc mật khẩu không đúng");
        }
    }

    /**
     * Get current user data
     * @param userId User ID
     * @return UserEntity or null if not found
     */
    public UserEntity getCurrentUser(int userId) {
        return userRepository.getUserById(userId);
    }

    /**
     * Update user profile
     * @param user Updated user data
     * @return true if update successful
     */
    public boolean updateUserProfile(UserEntity user) {
        try {
            userRepository.updateUser(user);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if string is email format
     */
    private boolean isEmail(String input) {
        return input != null && input.contains("@") && input.contains(".");
    }

    /**
     * Result class for registration
     */
    public static class RegisterResult {
        public final boolean success;
        public final String message;
        public final UserEntity user;

        public RegisterResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.user = null;
        }

        public RegisterResult(boolean success, String message, UserEntity user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }
    }

    /**
     * Result class for login
     */
    public static class LoginResult {
        public final boolean success;
        public final String message;
        public final UserEntity user;

        public LoginResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.user = null;
        }

        public LoginResult(boolean success, String message, UserEntity user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }
    }
}