package com.example.spending_management_app.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "User", indices = {@Index(value = "Email", unique = true)})
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "UserID")
    public int userId;

    @ColumnInfo(name = "FirstName")
    public String firstName;

    @ColumnInfo(name = "LastName")
    public String lastName;

    @ColumnInfo(name = "Email")
    public String email;

    @ColumnInfo(name = "Password")
    public String password;

    @ColumnInfo(name = "JoinDate")
    public String joinDate;

    @ColumnInfo(name = "AvatarUri")
    public String avatarUri;
}
