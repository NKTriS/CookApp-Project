package com.example.cookapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cookapp.data.local.entity.FavoriteEntity;
import com.example.cookapp.data.local.entity.ReviewEntity;

import java.util.List;

@Dao
public interface FavoriteReviewDao {

    // Favorites
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addFavorite(FavoriteEntity fav);

    @Query("DELETE FROM favorites WHERE recipe_id = :recipeId")
    void removeFavorite(int recipeId);

    @Query("DELETE FROM favorites WHERE recipe_id = :recipeId AND user_id = :userId")
    void removeFavoriteForUser(int recipeId, int userId);

    @Query("SELECT COUNT(*) FROM favorites WHERE recipe_id = :recipeId")
    int isFavorite(int recipeId);

    @Query("SELECT COUNT(*) FROM favorites WHERE recipe_id = :recipeId AND user_id = :userId")
    int isFavoriteForUser(int recipeId, int userId);

    @Query("SELECT * FROM favorites")
    List<FavoriteEntity> getAllFavorites();

    @Query("SELECT * FROM favorites WHERE user_id = :userId")
    List<FavoriteEntity> getFavoritesByUser(int userId);

    @Query("SELECT COUNT(*) FROM favorites WHERE user_id = :userId")
    int countFavoritesByUser(int userId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addFavoriteForUser(FavoriteEntity fav);

    // Reviews
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReview(ReviewEntity review);

    @Query("SELECT * FROM reviews WHERE recipe_id = :recipeId ORDER BY id DESC")
    List<ReviewEntity> getReviewsByRecipe(int recipeId);

    @Query("SELECT AVG(rating) FROM reviews WHERE recipe_id = :recipeId")
    float getAverageRating(int recipeId);

    @Query("SELECT COUNT(*) FROM reviews WHERE recipe_id = :recipeId")
    int getReviewCount(int recipeId);

    @Query("SELECT COUNT(*) FROM reviews WHERE user_id = :userId")
    int countReviewsByUser(int userId);

    @Query("SELECT * FROM reviews WHERE recipe_id = :recipeId AND user_id = :userId LIMIT 1")
    ReviewEntity getReviewByUserAndRecipe(int recipeId, int userId);

    @Query("UPDATE reviews SET rating = :rating, comment = :comment WHERE recipe_id = :recipeId AND user_id = :userId")
    void updateReviewForUser(int recipeId, int userId, int rating, String comment);

    @Query("DELETE FROM reviews WHERE recipe_id = :recipeId AND user_id = :userId")
    void deleteReviewForUser(int recipeId, int userId);

    @Query("DELETE FROM favorites")
    void deleteAllFavorites();

    @Query("DELETE FROM reviews")
    void deleteAllReviews();
}
