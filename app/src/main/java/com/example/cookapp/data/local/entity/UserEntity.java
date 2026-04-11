package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String email;
    public String password;
    public String fullName;
    public String phoneNumber;
    public String address;
    public String avatarUrl;
    public long createdAt;
}
