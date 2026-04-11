package com.example.cookapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cookapp.data.local.entity.UserEntity;

@Dao
public interface UserDao {
    @Insert
    long insertUser(UserEntity user);
    
    @Update
    void updateUser(UserEntity user);

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    UserEntity login(String email, String password);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity getUserByEmail(String email);
    
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    UserEntity getUserById(int userId);
}
