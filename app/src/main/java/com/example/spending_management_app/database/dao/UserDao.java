package com.example.spending_management_app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.spending_management_app.database.entity.UserEntity;

@Dao
public interface UserDao {
    @Insert
    long insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Query("SELECT * FROM User WHERE Email = :email AND Password = :password")
    UserEntity findByUser(String email, String password);

    @Query("SELECT * FROM User WHERE Email = :email")
    UserEntity findUserByEmail(String email);

    @Query("SELECT * FROM User WHERE UserID = :userId")
    UserEntity findUserById(int userId);
}
