package com.example.cookapp.repository;

import android.content.Context;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.GenericResponse;
import com.example.cookapp.api.dto.PostDto;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityRepository {
    private final ApiService apiService;

    public CommunityRepository(Context context) {
        apiService = RetrofitClient.getClient(context).create(ApiService.class);
    }

    public interface StateCallback<T> {
        void onResult(Resource<T> result);
    }

    public void getCommunityPosts(StateCallback<List<PostDto>> callback) {
        callback.onResult(Resource.loading(null));
        apiService.getCommunityPosts().enqueue(new Callback<List<PostDto>>() {
            @Override
            public void onResponse(Call<List<PostDto>> call, Response<List<PostDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Lỗi lấy bài viết: " + response.message(), null));
                }
            }

            @Override
            public void onFailure(Call<List<PostDto>> call, Throwable t) {
                callback.onResult(Resource.error("Lỗi kết nối mạng: " + t.getMessage(), null));
            }
        });
    }

    public void createPost(PostDto newPost, StateCallback<PostDto> callback) {
        callback.onResult(Resource.loading(null));
        apiService.createPost(newPost).enqueue(new Callback<PostDto>() {
            @Override
            public void onResponse(Call<PostDto> call, Response<PostDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Không thể đăng bài.", null));
                }
            }

            @Override
            public void onFailure(Call<PostDto> call, Throwable t) {
                callback.onResult(Resource.error("Lỗi đăng bài: " + t.getMessage(), null));
            }
        });
    }

    public void addComment(int postId, PostDto.CommentDto comment, StateCallback<PostDto.CommentDto> callback) {
        apiService.addComment(postId, comment).enqueue(new Callback<PostDto.CommentDto>() {
            @Override
            public void onResponse(Call<PostDto.CommentDto> call, Response<PostDto.CommentDto> response) {
                if(response.isSuccessful()) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Bình luận thất bại", null));
                }
            }
            @Override
            public void onFailure(Call<PostDto.CommentDto> call, Throwable t) {
                callback.onResult(Resource.error(t.getMessage(), null));
            }
        });
    }

    public void toggleLike(int postId, StateCallback<com.example.cookapp.api.dto.ToggleLikeResponse> callback) {
        apiService.toggleLike(postId).enqueue(new Callback<com.example.cookapp.api.dto.ToggleLikeResponse>() {
            @Override
            public void onResponse(Call<com.example.cookapp.api.dto.ToggleLikeResponse> call, Response<com.example.cookapp.api.dto.ToggleLikeResponse> response) {
                if(response.isSuccessful() && response.body() != null) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Lỗi", null));
                }
            }
            @Override
            public void onFailure(Call<com.example.cookapp.api.dto.ToggleLikeResponse> call, Throwable t) {
                callback.onResult(Resource.error("Lỗi", null));
            }
        });
    }

    /** Lấy bài viết của user đang đăng nhập từ GET /api/community/posts/mine */
    public void getMyPosts(StateCallback<List<PostDto>> callback) {
        callback.onResult(Resource.loading(null));
        apiService.getMyPosts().enqueue(new Callback<List<PostDto>>() {
            @Override
            public void onResponse(Call<List<PostDto>> call, Response<List<PostDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(Resource.success(response.body()));
                } else {
                    callback.onResult(Resource.error("Lỗi lấy bài viết: " + response.code(), null));
                }
            }
            @Override
            public void onFailure(Call<List<PostDto>> call, Throwable t) {
                callback.onResult(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
    }
}
