package com.example.cookapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cookapp.data.local.entity.PostCommentEntity;
import com.example.cookapp.data.local.entity.PostEntity;
import com.example.cookapp.data.local.entity.PostLikeEntity;

import java.util.List;

@Dao
public interface CommunityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPost(PostEntity post);

    @Query("SELECT * FROM posts ORDER BY created_at DESC")
    List<PostEntity> getAllPosts();

    @Query("SELECT * FROM posts WHERE user_id = :userId ORDER BY created_at DESC")
    List<PostEntity> getPostsByUser(int userId);

    @Query("SELECT * FROM posts WHERE id = :postId")
    PostEntity getPostById(int postId);

    @Query("DELETE FROM posts WHERE id = :postId")
    void deletePostById(int postId);

    @Query("UPDATE posts SET likes = likes + 1 WHERE id = :postId")
    void incrementLikes(int postId);

    @Query("UPDATE posts SET likes = MAX(0, likes - 1) WHERE id = :postId")
    void decrementLikes(int postId);

    @Insert
    void insertComment(PostCommentEntity comment);

    @Query("SELECT * FROM post_comments WHERE post_id = :postId ORDER BY created_at ASC")
    List<PostCommentEntity> getCommentsByPost(int postId);

    @Query("SELECT COUNT(*) FROM post_comments WHERE user_id = :userId")
    int countCommentsByUser(int userId);

    @Query("DELETE FROM posts")
    void deleteAllPosts();

    @Query("DELETE FROM post_comments")
    void deleteAllComments();

    // ── Like tracking ────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertLike(PostLikeEntity like);

    @Query("DELETE FROM post_likes WHERE user_id = :userId AND post_id = :postId")
    void deleteLike(int userId, int postId);

    @Query("SELECT COUNT(*) FROM post_likes WHERE user_id = :userId AND post_id = :postId")
    int hasLiked(int userId, int postId);

    @Query("DELETE FROM post_likes")
    void deleteAllLikes();
}
