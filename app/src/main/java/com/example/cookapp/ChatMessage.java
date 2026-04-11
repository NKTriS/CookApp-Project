package com.example.cookapp;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;

    private String content;
    private int type;

    public ChatMessage(String content, int type) {
        this.content = content;
        this.type = type;
    }

    public String getContent() { return content; }
    public int getType() { return type; }
    public void setContent(String content) { this.content = content; }

    public String getRole() {
        return type == TYPE_USER ? "user" : "bot";
    }
}
