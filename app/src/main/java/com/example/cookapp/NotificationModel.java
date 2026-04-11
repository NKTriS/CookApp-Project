package com.example.cookapp;

public class NotificationModel {
    private String id;
    private String timeAgo;
    private String message;
    private boolean read;
    private String type;

    public NotificationModel(String id, String timeAgo, String message, boolean read, String type) {
        this.id = id;
        this.timeAgo = timeAgo;
        this.message = message;
        this.read = read;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTimeAgo() { return timeAgo; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
