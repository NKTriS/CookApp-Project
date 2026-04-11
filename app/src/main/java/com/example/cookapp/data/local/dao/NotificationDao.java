package com.example.cookapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.cookapp.data.local.entity.NotificationEntity;

import java.util.List;

@Dao
public interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNotification(NotificationEntity notification);

    /** Get all notifications for a user, newest first */
    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC")
    List<NotificationEntity> getNotificationsByUser(int userId);

    /** Count unread notifications for a user */
    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0")
    int getUnreadCount(int userId);

    /** Mark a single notification as read */
    @Query("UPDATE notifications SET is_read = 1 WHERE id = :notificationId")
    void markAsRead(int notificationId);

    /** Mark ALL notifications for a user as read */
    @Query("UPDATE notifications SET is_read = 1 WHERE user_id = :userId")
    void markAllAsRead(int userId);

    /** Delete all notifications for a user */
    @Query("DELETE FROM notifications WHERE user_id = :userId")
    void deleteAllForUser(int userId);

    /** Delete all (used during full DB clear/seed) */
    @Query("DELETE FROM notifications")
    void deleteAll();
}
