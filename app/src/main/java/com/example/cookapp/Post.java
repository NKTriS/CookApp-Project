package com.example.cookapp;

public class Post {
    private String id;
    private String author;
    private String timeAgo;
    private String content;
    private int imgResId;
    private int likes;
    private int comments;
    private boolean liked;
    private String imageUri;
    private boolean saved;

    public Post(String id, String author, String timeAgo, String content, int imgResId, int likes, int comments, boolean liked, String imageUri) {
        this.id = id;
        this.author = author;
        this.timeAgo = timeAgo;
        this.content = content;
        this.imgResId = imgResId;
        this.likes = likes;
        this.comments = comments;
        this.liked = liked;
        this.imageUri = imageUri;
        this.saved = false;
    }

    public Post(String id, String author, String timeAgo, String content, int imgResId, int likes, int comments, boolean liked, String imageUri, boolean saved) {
        this.id = id;
        this.author = author;
        this.timeAgo = timeAgo;
        this.content = content;
        this.imgResId = imgResId;
        this.likes = likes;
        this.comments = comments;
        this.liked = liked;
        this.imageUri = imageUri;
        this.saved = saved;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getTimeAgo() { return timeAgo; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getImgResId() { return imgResId; }
    public void setImgResId(int imgResId) { this.imgResId = imgResId; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public int getComments() { return comments; }
    public void setComments(int comments) { this.comments = comments; }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }
    public boolean isSaved() { return saved; }
    public void setSaved(boolean saved) { this.saved = saved; }
}
