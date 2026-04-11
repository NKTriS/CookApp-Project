package com.example.cookapp.api.dto;

import java.util.List;

public class PostDto {
    public int id;
    public String title;
    public String content;
    public String author;
    public int user_id;
    public String image_url;
    public int likes;
    public String created_at;
    public boolean is_liked_by_me;  // trạng thái tim của user hiện tại
    public boolean is_saved_by_me;  // trạng thái lưu bài viết

    public List<CommentDto> comments;

    public static class CommentDto {
        public int id;
        public String content;
        public String author;
        public String created_at;
        public int user_id;
    }
}
