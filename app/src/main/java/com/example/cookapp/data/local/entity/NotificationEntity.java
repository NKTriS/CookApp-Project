package com.example.cookapp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications")
public class NotificationEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int user_id;        // Which user this notification belongs to

    public String message;     // Notification text

    public String type;        // "like", "comment", "system", "recipe"

    @ColumnInfo(name = "is_read")
    public boolean isRead;     // Has the user seen it?

    public long created_at;    // Timestamp millis
}
