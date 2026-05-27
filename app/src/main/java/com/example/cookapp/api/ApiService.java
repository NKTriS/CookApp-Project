package com.example.cookapp.api;

import com.example.cookapp.Recipe;
import com.example.cookapp.Review;
import com.example.cookapp.api.dto.AuthRequest;
import com.example.cookapp.api.dto.AuthResponse;
import com.example.cookapp.api.dto.CheckIngredientsRequest;
import com.example.cookapp.api.dto.FavoriteDto;
import com.example.cookapp.api.dto.GenericResponse;
import com.example.cookapp.api.dto.NotificationDto;
import com.example.cookapp.api.dto.PostDto;
import com.example.cookapp.api.dto.RegisterRequest;
import com.example.cookapp.api.dto.ReviewsResponse;
import com.example.cookapp.api.dto.ShoppingListResponse;
import com.example.cookapp.api.dto.StoreProductDto;
import com.example.cookapp.api.dto.SyncShoppingListRequest;
import com.example.cookapp.api.dto.SyncShoppingListRequest.ShoppingItem;
import com.example.cookapp.api.dto.ToggleFavoriteRequest;
import com.example.cookapp.api.dto.ToggleFavoriteResponse;
import com.example.cookapp.api.dto.UserDto;
import com.example.cookapp.api.dto.OrderDto;
import com.example.cookapp.api.dto.CreateOrderRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Multipart;
import retrofit2.http.Part;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Khai báo toàn bộ API Retrofit mà ứng dụng Android gọi tới backend CookApp.
 *
 * Các annotation @GET, @POST, @PATCH, @DELETE mô tả endpoint tương ứng.
 * RetrofitClient chịu trách nhiệm cấu hình baseUrl và tự gắn Authorization header
 * cho những API cần đăng nhập hoặc cần quyền admin.
 */
public interface ApiService {

    // --- AUTH ---
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @GET("api/auth/me")
    Call<UserDto> getMe();

    @PATCH("api/auth/profile")
    Call<GenericResponse> updateProfile(@Body UserDto profileUpdate);

    @GET("api/profile/stats")
    Call<com.example.cookapp.api.dto.ProfileStatsDto> getProfileStats();

    // --- RECIPES ---
    @GET("api/recipes")
    Call<List<Recipe>> getAllRecipes();

    @GET("api/recipes")
    Call<List<Recipe>> getRecipesByQuery(
        @Query("search") String search,
        @Query("category") Integer category,
        @Query("filters") String filters
    );

    @GET("api/recipes/category/{id}")
    Call<List<Recipe>> getRecipesByCategory(@Path("id") int categoryId);

    @GET("api/recipes/{id}")
    Call<Recipe> getRecipeDetail(@Path("id") int recipeId);

    /** Lấy các bước nấu, timer_seconds và video_start_time cho Cooking Mode. */
    @GET("api/recipes/{id}/steps")
    Call<List<com.example.cookapp.api.dto.RecipeStepDto>> getRecipeSteps(@Path("id") int recipeId);

    @GET("api/recipes/{id}/ingredients")
    Call<List<com.example.cookapp.api.dto.RecipeIngredientDto>> getRecipeIngredients(@Path("id") int recipeId);

    @GET("api/recipes/{id}/nutrition")
    Call<com.example.cookapp.api.dto.NutritionDto> getRecipeNutrition(@Path("id") int recipeId);

    @GET("api/recipes/recommendations")
    Call<List<Recipe>> getRecommendations();

    @POST("api/recipes/smart-fridge")
    Call<List<Recipe>> getSmartFridgeRecommendations(@Body com.example.cookapp.api.SmartFridgeRequest request);

    // --- REVIEWS ---
    @GET("api/recipes/{id}/reviews")
    Call<ReviewsResponse> getRecipeReviews(@Path("id") int recipeId);

    @POST("api/recipes/{id}/reviews")
    Call<Review> createReview(@Path("id") int recipeId, @Body Review newReview);

    @DELETE("api/recipes/{recipeId}/reviews/{reviewId}")
    Call<GenericResponse> deleteReview(
        @Path("recipeId")  int recipeId,
        @Path("reviewId")  int reviewId
    );

    // --- COMMUNITY ---
    @GET("api/community/posts")
    Call<List<PostDto>> getCommunityPosts();

    @GET("api/community/posts/{id}")
    Call<PostDto> getPostDetail(@Path("id") int postId);

    @POST("api/community/posts")
    Call<PostDto> createPost(@Body PostDto newPost);

    @POST("api/community/posts/{id}/comments")
    Call<PostDto.CommentDto> addComment(@Path("id") int postId, @Body PostDto.CommentDto comment);

    @PUT("api/community/comments/{id}")
    Call<PostDto.CommentDto> updateComment(@Path("id") int commentId, @Body PostDto.CommentDto comment);

    @DELETE("api/community/comments/{id}")
    Call<GenericResponse> deleteComment(@Path("id") int commentId);

    @POST("api/community/posts/{id}/like")
    Call<com.example.cookapp.api.dto.ToggleLikeResponse> toggleLike(@Path("id") int postId);

    @GET("api/community/posts/mine")
    Call<List<PostDto>> getMyPosts();

    @POST("api/community/posts/{id}/save")
    Call<GenericResponse> toggleSavePost(@Path("id") int postId);

    @GET("api/community/posts/saved")
    Call<List<PostDto>> getSavedPosts();

    // --- USER: FAVORITES (Requires Bearer Token) ---
    @GET("api/favorites")
    Call<List<FavoriteDto>> getFavorites();

    @GET("api/favorites/check/{recipeId}")
    Call<ToggleFavoriteResponse> checkFavorite(@Path("recipeId") int recipeId);

    @POST("api/favorites/toggle")
    Call<ToggleFavoriteResponse> toggleFavorite(@Body ToggleFavoriteRequest request);

    // --- USER: SHOPPING LIST ---
    @GET("api/shopping-list")
    Call<ShoppingListResponse> getShoppingList();

    @POST("api/shopping-list/sync")
    Call<GenericResponse> syncShoppingList(@Body SyncShoppingListRequest request);

    @POST("api/shopping-list/items")
    Call<GenericResponse> addShoppingItem(@Body ShoppingItem item);

    // --- USER: NOTIFICATIONS ---
    @GET("api/notifications")
    Call<List<NotificationDto>> getNotifications();

    @POST("api/notifications/{id}/read")
    Call<NotificationDto> markNotificationAsRead(@Path("id") int notificationId);

    // --- STORE PRODUCTS ---
    /** Tìm sản phẩm theo tên nguyên liệu và/hoặc cửa hàng */
    @GET("api/store-products")
    Call<List<StoreProductDto>> searchStoreProducts(
        @Query("q") String ingredientName,
        @Query("store") String storeName
    );

    /** Kiểm tra nhiều nguyên liệu → Map có sẵn không */
    @POST("api/store-products/check-ingredients")
    Call<Map<String, List<StoreProductDto>>> checkIngredients(@Body CheckIngredientsRequest request);

    // --- AI CHATBOT ---
    /** Gửi tin nhắn tới Chef AI; backend sẽ gọi Groq API và trả về trường reply. */
    @POST("api/chat")
    Call<ResponseBody> sendChatMessage(@Body RequestBody body);

    // --- ORDERS (Server-side) ---
    @GET("api/orders")
    Call<List<OrderDto>> getOrders();

    @GET("api/orders/{id}")
    Call<OrderDto> getOrderDetail(@Path("id") int orderId);

    @POST("api/orders")
    Call<OrderDto> createOrder(@Body CreateOrderRequest request);

    @PATCH("api/orders/{id}/cancel")
    Call<OrderDto> cancelOrder(@Path("id") int orderId, @Body java.util.Map<String, String> body);

    // --- PAYMENT STATUS (SePay) ---
    @GET("api/payment/status/{orderId}")
    Call<com.example.cookapp.api.dto.PaymentStatusResponse> getPaymentStatus(@Path("orderId") int orderId);

    // --- VNPAY ---
    @POST("api/payment/vnpay/create_url")
    Call<com.example.cookapp.api.dto.VnpayUrlResponse> createVnpayUrl(@Body java.util.Map<String, Object> body);

    // --- ADMIN ---
    /** Lấy số liệu tổng quan hiển thị trên dashboard admin. */
    @GET("api/admin/stats")
    Call<com.example.cookapp.api.dto.AdminStatsDto> getAdminStats();

    /** Lấy danh sách đơn hàng, có phân trang và lọc theo trạng thái. */
    @GET("api/admin/orders")
    Call<com.example.cookapp.api.dto.AdminOrdersResponse> getAdminOrders(
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("status") String status
    );

    /** Cập nhật trạng thái xử lý/giao hàng của một đơn hàng. */
    @PATCH("api/admin/orders/{id}/status")
    Call<OrderDto> updateAdminOrderStatus(@Path("id") int orderId, @Body java.util.Map<String, String> body);

    /** Tìm kiếm và lấy danh sách người dùng cho màn hình quản trị. */
    @GET("api/admin/users")
    Call<com.example.cookapp.api.dto.AdminUsersResponse> getAdminUsers(
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("search") String search
    );

    /** Đổi vai trò của người dùng giữa user và admin. */
    @PATCH("api/admin/users/{id}/role")
    Call<GenericResponse> updateUserRole(@Path("id") int userId, @Body java.util.Map<String, String> body);

    /** Xóa tài khoản người dùng khỏi hệ thống. */
    @DELETE("api/admin/users/{id}")
    Call<GenericResponse> deleteUser(@Path("id") int userId);

    /** Lấy danh sách công thức để admin quản lý nội dung. */
    @GET("api/admin/recipes")
    Call<com.example.cookapp.api.dto.AdminRecipesResponse> getAdminRecipes(
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("search") String search
    );

    @DELETE("api/admin/recipes/{id}")
    Call<GenericResponse> deleteAdminRecipe(@Path("id") int recipeId);

    @GET("api/admin/posts")
    Call<com.example.cookapp.api.dto.AdminPostsResponse> getAdminPosts(
        @Query("page") int page,
        @Query("limit") int limit
    );

    @DELETE("api/admin/posts/{id}")
    Call<GenericResponse> deleteAdminPost(@Path("id") int postId);

    @GET("api/admin/reviews")
    Call<com.example.cookapp.api.dto.AdminReviewsResponse> getAdminReviews(
        @Query("page") int page,
        @Query("limit") int limit
    );

    @DELETE("api/admin/reviews/{id}")
    Call<GenericResponse> deleteAdminReview(@Path("id") int reviewId);

    /** Lấy category, diet type và ingredient để dựng form thêm/sửa công thức. */
    @GET("api/admin/recipe-metadata")
    Call<com.example.cookapp.api.dto.RecipeMetadataResponse> getRecipeMetadata();

    @POST("api/admin/recipes/{id}/auto-sync-video")
    Call<com.example.cookapp.api.dto.GenericResponse> autoSyncVideo(@Path("id") int recipeId);

    @PUT("api/admin/recipes/{id}/steps")
    Call<com.example.cookapp.api.dto.GenericResponse> updateRecipeSteps(
        @Path("id") int recipeId, 
        @Body com.example.cookapp.api.dto.AdminStepsUpdateRequest request
    );

    /** Tạo công thức mới bằng multipart gồm JSON data, ảnh đại diện và video hướng dẫn. */
    @Multipart
    @POST("api/admin/recipes")
    Call<com.example.cookapp.api.dto.GenericResponse> createRecipe(
        @Part("data") okhttp3.RequestBody dtoData,
        @Part okhttp3.MultipartBody.Part image,
        @Part okhttp3.MultipartBody.Part video
    );
}
