package com.example.cookapp;

public class Comment {
    private String id;
    private String author;
    private String date;
    private String text;
    private boolean isOwn;

    public Comment(String id, String author, String date, String text, boolean isOwn) {
        this.id = id;
        this.author = author;
        this.date = date;
        this.text = text;
        this.isOwn = isOwn;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isOwn() { return isOwn; }
    public void setOwn(boolean own) { isOwn = own; }
}
