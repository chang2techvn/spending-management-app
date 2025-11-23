package com.example.spending_management_app.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String emailOrPhone; // Có thể là email hoặc số điện thoại
    public String passwordHash; // Mật khẩu đã được mã hóa
    public String name; // Tên hiển thị
    public String avatar; // URL hoặc path của avatar
    public Date createdAt;

    // Constructor mặc định cho Room
    public UserEntity() {}

    public UserEntity(String emailOrPhone, String passwordHash, String name, String avatar) {
        this.emailOrPhone = emailOrPhone;
        this.passwordHash = passwordHash;
        this.name = name;
        this.avatar = avatar;
        this.createdAt = new Date();
    }

    // Getters
    public int getId() { return id; }
    public String getEmailOrPhone() { return emailOrPhone; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public String getAvatar() { return avatar; }
    public Date getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setEmailOrPhone(String emailOrPhone) { this.emailOrPhone = emailOrPhone; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setName(String name) { this.name = name; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}