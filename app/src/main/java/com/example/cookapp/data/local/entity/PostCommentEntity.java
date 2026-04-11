package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "post_comments")
public class PostCommentEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int user_id;
    public int post_id;
    public String author;
    public String content;
    public long created_at; // Timestamp
}
