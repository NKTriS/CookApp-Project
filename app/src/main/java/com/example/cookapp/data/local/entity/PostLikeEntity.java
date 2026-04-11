package com.example.cookapp.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;

/**
 * Tracks which user liked which post.
 * (user_id, post_id) is unique — each user can only like a post once.
 */
@Entity(
    tableName = "post_likes",
    primaryKeys = {"user_id", "post_id"},
    indices = {@Index(value = {"user_id", "post_id"}, unique = true)}
)
public class PostLikeEntity {
    public int user_id;
    public int post_id;
    public long liked_at;
}
