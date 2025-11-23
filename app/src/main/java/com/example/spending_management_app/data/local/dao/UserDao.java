package com.example.spending_management_app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.spending_management_app.data.local.entity.UserEntity;

@Dao
public interface UserDao {
    @Insert
    long insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Query("SELECT * FROM users WHERE emailOrPhone = :emailOrPhone LIMIT 1")
    UserEntity getUserByEmailOrPhone(String emailOrPhone);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserEntity getUserById(int id);

    @Query("SELECT COUNT(*) FROM users WHERE emailOrPhone = :emailOrPhone")
    int getUserCountByEmailOrPhone(String emailOrPhone);

    @Query("SELECT * FROM users ORDER BY createdAt DESC LIMIT 1")
    UserEntity getLatestUser();
}