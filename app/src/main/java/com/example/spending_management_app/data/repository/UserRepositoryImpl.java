package com.example.spending_management_app.data.repository;

import com.example.spending_management_app.data.local.database.AppDatabase;
import com.example.spending_management_app.data.local.entity.UserEntity;
import com.example.spending_management_app.domain.repository.UserRepository;
import com.example.spending_management_app.utils.PasswordUtils;

/**
 * Implementation of UserRepository
 */
public class UserRepositoryImpl implements UserRepository {

    private final AppDatabase appDatabase;

    public UserRepositoryImpl(AppDatabase appDatabase) {
        this.appDatabase = appDatabase;
    }

    @Override
    public long registerUser(UserEntity user) {
        try {
            return appDatabase.userDao().insert(user);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public UserEntity loginUser(String emailOrPhone, String password) {
        UserEntity user = appDatabase.userDao().getUserByEmailOrPhone(emailOrPhone);
        if (user != null && PasswordUtils.verifyPassword(password, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    @Override
    public UserEntity getUserByEmailOrPhone(String emailOrPhone) {
        return appDatabase.userDao().getUserByEmailOrPhone(emailOrPhone);
    }

    @Override
    public UserEntity getUserById(int id) {
        return appDatabase.userDao().getUserById(id);
    }

    @Override
    public void updateUser(UserEntity user) {
        appDatabase.userDao().update(user);
    }

    @Override
    public boolean isUserExists(String emailOrPhone) {
        return appDatabase.userDao().getUserCountByEmailOrPhone(emailOrPhone) > 0;
    }
}