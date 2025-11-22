package com.example.spending_management_app.domain.repository;

import com.example.spending_management_app.data.local.entity.UserEntity;

/**
 * Repository interface for user authentication operations
 */
public interface UserRepository {

    /**
     * Register a new user
     * @param user The user entity to register
     * @return The ID of the newly created user, or -1 if registration failed
     */
    long registerUser(UserEntity user);

    /**
     * Authenticate user login
     * @param emailOrPhone Email or phone number
     * @param password Plain text password
     * @return UserEntity if authentication successful, null otherwise
     */
    UserEntity loginUser(String emailOrPhone, String password);

    /**
     * Get user by email or phone
     * @param emailOrPhone Email or phone number
     * @return UserEntity if found, null otherwise
     */
    UserEntity getUserByEmailOrPhone(String emailOrPhone);

    /**
     * Get user by ID
     * @param id User ID
     * @return UserEntity if found, null otherwise
     */
    UserEntity getUserById(int id);

    /**
     * Update user information
     * @param user The user entity to update
     */
    void updateUser(UserEntity user);

    /**
     * Check if email or phone is already registered
     * @param emailOrPhone Email or phone number
     * @return true if exists, false otherwise
     */
    boolean isUserExists(String emailOrPhone);
}