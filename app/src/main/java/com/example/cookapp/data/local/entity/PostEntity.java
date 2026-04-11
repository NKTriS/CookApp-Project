package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "posts")
public class PostEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String title;
    public String content;
    public String author;
    public int user_id;
    public long created_at; // Timestamp
    public int likes;
    public String image_uri; // Local URI for dynamically uploaded posts
}
