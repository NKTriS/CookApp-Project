package com.example.cookapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.dto.SyncShoppingListRequest;
import com.example.cookapp.data.local.AppDatabase;
import com.example.cookapp.data.local.entity.NutritionFactEntity;
import com.example.cookapp.data.local.entity.RecipeEntity;
import com.example.cookapp.data.local.entity.RecipeIngredientEntity;
import com.example.cookapp.data.local.entity.RecipeStepEntity;
import com.example.cookapp.data.local.entity.ShoppingListItemEntity;
import com.example.cookapp.repository.ShoppingRepository;
import com.example.cookapp.repository.UserRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecipeDetailActivity extends AppCompatActivity {

    // Dữ liệu gốc để scale nguyên liệu
    private List<RecipeIngredientEntity> baseIngredients = new ArrayList<>();
    private Map<Integer, String> ingredientNames = new HashMap<>();
    private int baseServings = 4;
    private int currentServings = 4;
    private LinearLayout llIngredients;
    private LinearLayout llShoppingItems;
    private RecipeEntity loadedRecipe;
    private NutritionFactEntity loadedNutrition;
    private boolean isFavorite = false;
    private int recipeId = -1;

    // Video player
    private ExoPlayer exoPlayer;
    private PlayerView playerView;
    private FrameLayout videoThumbnailContainer;

    private UserRepository userRepo;
    private ShoppingRepository shoppingRepo;

    @androidx.media3.common.util.UnstableApi
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipe_detail);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        recipeId = getIntent().getIntExtra("recipe_id", 1);

        userRepo = new UserRepository(this);
        shoppingRepo = new ShoppingRepository(this);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        // ── Favorite toggle (API-based) ────────────────────────────────────
        ImageView ivFavorite = findViewById(R.id.iv_favorite);
        SessionManager session = new SessionManager(this);
        if (ivFavorite != null && session.isLoggedIn()) {
            // Load trạng thái ban đầu từ API
            userRepo.checkFavorite(recipeId, result -> {
                if (result.status == Resource.Status.SUCCESS && result.data != null) {
                    isFavorite = result.data;
                    runOnUiThread(() -> ivFavorite.setImageResource(
                        isFavorite ? android.R.drawable.btn_star_big_on : R.drawable.ic_star_outline));
                }
            });
            ivFavorite.setOnClickListener(v -> {
                userRepo.toggleFavorite(recipeId, result -> {
                    if (result.status == Resource.Status.SUCCESS && result.data != null) {
                        isFavorite = result.data.isFavorite;
                        runOnUiThread(() -> {
                            ivFavorite.setImageResource(
                                isFavorite ? android.R.drawable.btn_star_big_on : R.drawable.ic_star_outline);
                            Toast.makeText(this,
                                isFavorite ? "Đã thêm vào yêu thích ⭐" : "Đã xóa khỏi yêu thích",
                                Toast.LENGTH_SHORT).show();
                        });
                    } else if (result.status == Resource.Status.ERROR) {
                        runOnUiThread(() -> Toast.makeText(this,
                            "Lỗi cập nhật yêu thích: " + result.message, Toast.LENGTH_SHORT).show());
                    }
                });
            });
        } else if (ivFavorite != null) {
            ivFavorite.setOnClickListener(v -> {
                SessionManager.requireLogin(this);
            });
        }
        // Let's Cook → CookingModeActivity với recipe_id
        LinearLayout btnLetsCook = findViewById(R.id.btn_lets_cook);
        if (btnLetsCook != null) {
            btnLetsCook.setOnClickListener(v -> {
                Intent intent = new Intent(this, CookingModeActivity.class);
                intent.putExtra("recipe_id", recipeId);
                startActivity(intent);
            });
        }

        // Share Recipe
        ImageView ivShare = findViewById(R.id.iv_share);
        if (ivShare != null) {
            ivShare.setOnClickListener(v -> shareRecipe());
        }

        // Tab Ingredients / Nutrition
        TextView tabIngredients = findViewById(R.id.tab_ingredients);
        TextView tabNutrition   = findViewById(R.id.tab_nutrition);
        llIngredients = findViewById(R.id.ll_ingredients);
        LinearLayout llNutrition   = findViewById(R.id.ll_nutrition);

        if (tabIngredients != null && tabNutrition != null && llIngredients != null && llNutrition != null) {
            tabIngredients.setOnClickListener(v -> {
                tabIngredients.setBackgroundColor(Color.WHITE);
                tabNutrition.setBackgroundColor(Color.parseColor("#F5F5F5"));
                llIngredients.setVisibility(View.VISIBLE);
                llNutrition.setVisibility(View.GONE);
            });
            tabNutrition.setOnClickListener(v -> {
                tabNutrition.setBackgroundColor(Color.WHITE);
                tabIngredients.setBackgroundColor(Color.parseColor("#F5F5F5"));
                llNutrition.setVisibility(View.VISIBLE);
                llIngredients.setVisibility(View.GONE);
            });
        }

        // Servings +/- buttons
        TextView tvServeCount = findViewById(R.id.tv_serve_count);
        View btnServePlus  = findViewById(R.id.btn_serve_plus);
        View btnServeMinus = findViewById(R.id.btn_serve_minus);
        if (btnServePlus != null && tvServeCount != null) {
            btnServePlus.setOnClickListener(v -> {
                currentServings++;
                tvServeCount.setText(String.valueOf(currentServings));
                renderScaledIngredients();
            });
        }
        if (btnServeMinus != null && tvServeCount != null) {
            btnServeMinus.setOnClickListener(v -> {
                if (currentServings > 1) {
                    currentServings--;
                    tvServeCount.setText(String.valueOf(currentServings));
                    renderScaledIngredients();
                }
            });
        }

        LinearLayout llInlineShoppingList = findViewById(R.id.ll_inline_shopping_list);
        LinearLayout btnShoppingList = findViewById(R.id.btn_shopping_list);
        if (btnShoppingList != null && llInlineShoppingList != null) {
            btnShoppingList.setOnClickListener(v -> {
                llInlineShoppingList.setVisibility(
                    llInlineShoppingList.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            });
        }

        Button btnConfirmShopping = findViewById(R.id.btn_confirm_shopping);
        llShoppingItems = findViewById(R.id.ll_shopping_items);
        if (btnConfirmShopping != null && llShoppingItems != null) {
            btnConfirmShopping.setOnClickListener(v -> {
                // Thu thập checkbox đã check
                List<CartItem> newCartItems = new ArrayList<>();
                int selectedCount = 0;
                
                for (int i = 0; i < llShoppingItems.getChildCount(); i++) {
                    View child = llShoppingItems.getChildAt(i);
                    CheckBox cb = null;
                    if (child instanceof CheckBox) {
                        cb = (CheckBox) child;
                    } else if (child instanceof android.widget.LinearLayout) {
                        android.widget.LinearLayout rowLayout = (android.widget.LinearLayout) child;
                        for (int j = 0; j < rowLayout.getChildCount(); j++) {
                            View inner = rowLayout.getChildAt(j);
                            if (inner instanceof CheckBox) {
                                cb = (CheckBox) inner;
                                break;
                            }
                        }
                    }

                    if (cb == null || !cb.isEnabled() || !cb.isChecked()) continue;

                    Object tag = cb.getTag();
                    if (tag instanceof String) {
                        String[] parts = ((String) tag).split("\\|");
                        String ingName = parts.length > 1 ? parts[1] : cb.getText().toString();
                        int qty = 1;
                        try { qty = parts.length > 2 ? (int) Float.parseFloat(parts[2]) : 1; } catch (Exception ignored) { }
                        String unit = parts.length > 3 ? parts[3] : "";
                        int price = 0;
                        try { price = parts.length > 4 ? Integer.parseInt(parts[4]) : 0; } catch (Exception ignored) {}

                        String cartId = "ing_" + (parts.length > 0 ? parts[0] : i);
                        newCartItems.add(new CartItem(cartId, ingName, unit, price, qty));
                    } else {
                        newCartItems.add(new CartItem("ing_" + i, cb.getText().toString(), "", 0, 1));
                    }
                    selectedCount++;
                }
                
                if (newCartItems.isEmpty()) {
                    Toast.makeText(this, "Chọn ít nhất một nguyên liệu", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Kiểm tra đăng nhập
                SessionManager sessionShopping = new SessionManager(this);
                if (!sessionShopping.isLoggedIn()) {
                    Toast.makeText(this, "Vui lòng đăng nhập để lưu danh sách mua sắm", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Ẩn panel
                if (llInlineShoppingList != null)
                    llInlineShoppingList.setVisibility(View.GONE);

                // Add to Cart, which automatically syncs to Backend ShoppingList DB in bulk!
                CartManager.getInstance(this).addMultipleItems(newCartItems);

                Toast.makeText(this,
                    "✓ Đã thêm " + selectedCount + " nguyên liệu vào giỏ hàng!",
                    Toast.LENGTH_SHORT).show();
            });
        }

        LinearLayout btnRating = findViewById(R.id.btn_rating);
        if (btnRating != null) {
            btnRating.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReviewActivity.class);
                intent.putExtra("recipe_id", recipeId);
                startActivity(intent);
            });
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_recipe);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_recipe) {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    return true;
                }
                if (id == R.id.nav_shop) {
                    Intent intent = new Intent(this, ShopActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    return true;
                }
                if (id == R.id.nav_fav) {
                    if (SessionManager.requireLogin(RecipeDetailActivity.this)) return true;
                    Intent intent = new Intent(this, FavoritesActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    return true;
                }
                if (id == R.id.nav_settings) {
                    Intent intent = new Intent(this, SettingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }

        // ============================================================
        // Load dữ liệu đồng bộ với Cloud Backend rồi fetch từ Room
        // ============================================================
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);

            try {
                // Fetch synchronous priority from Backend API to fix ID collisions
                retrofit2.Response<com.example.cookapp.Recipe> res = com.example.cookapp.api.RetrofitClient
                    .getClient(this).create(com.example.cookapp.api.ApiService.class)
                    .getRecipeDetail(recipeId).execute();
                    
                if (res.isSuccessful() && res.body() != null) {
                    com.example.cookapp.Recipe r = res.body();
                    RecipeEntity re = db.recipeDao().getRecipeById(recipeId);
                    if (re == null) re = new RecipeEntity();
                    re.id = r.getId();
                    if (r.getTitle() != null) re.title = r.getTitle();
                    if (r.getDescription() != null) re.description = r.getDescription();
                    if (r.getImageUrl() != null) re.image_url = r.getImageUrl();
                    if (r.getVideoUrl() != null) re.video_url = r.getVideoUrl();
                    if (r.getVideoThumbnailUrl() != null) re.video_thumbnail_url = r.getVideoThumbnailUrl();
                    re.imageSource = "CookApp Cloud";
                    try { re.cook_time = Integer.parseInt(r.getTime() != null ? r.getTime() : "0"); } catch(Exception e){}
                    try { re.calories = Integer.parseInt(r.getCalories() != null ? r.getCalories() : "0"); } catch(Exception e){}
                    if (r.getDifficulty() != null) re.difficulty = r.getDifficulty();
                    if (r.getServings() > 0) re.servings = r.getServings();
                    
                    db.recipeDao().insertRecipe(re);
                }

                // =================Deep Sync=================
                // SYNC STEPS
                retrofit2.Response<List<com.example.cookapp.api.dto.RecipeStepDto>> stepRes = com.example.cookapp.api.RetrofitClient
                    .getClient(this).create(com.example.cookapp.api.ApiService.class)
                    .getRecipeSteps(recipeId).execute();
                if (stepRes.isSuccessful() && stepRes.body() != null) {
                    db.recipeDao().deleteStepsByRecipe(recipeId);
                    for (com.example.cookapp.api.dto.RecipeStepDto dto : stepRes.body()) {
                        com.example.cookapp.data.local.entity.RecipeStepEntity sEntity = new com.example.cookapp.data.local.entity.RecipeStepEntity();
                        sEntity.recipe_id = recipeId;
                        sEntity.step_number = dto.step_number;
                        sEntity.instruction = dto.instruction;
                        sEntity.title = dto.title;
                        sEntity.timer_seconds = dto.timer_seconds;
                        db.recipeDao().insertStep(sEntity);
                    }
                }

                // SYNC INGREDIENTS
                retrofit2.Response<List<com.example.cookapp.api.dto.RecipeIngredientDto>> ingRes = com.example.cookapp.api.RetrofitClient
                    .getClient(this).create(com.example.cookapp.api.ApiService.class)
                    .getRecipeIngredients(recipeId).execute();
                if (ingRes.isSuccessful() && ingRes.body() != null) {
                    db.recipeDao().deleteIngredientsByRecipe(recipeId);
                    for (com.example.cookapp.api.dto.RecipeIngredientDto dto : ingRes.body()) {
                        com.example.cookapp.data.local.entity.RecipeIngredientEntity iEntity = new com.example.cookapp.data.local.entity.RecipeIngredientEntity();
                        iEntity.recipe_id = recipeId;
                        iEntity.ingredient_id = dto.id;
                        iEntity.quantity = dto.getQuantity();
                        iEntity.unit = dto.getUnit();
                        db.recipeDao().insertRecipeIngredient(iEntity);
                    }
                }

                // SYNC NUTRITION
                retrofit2.Response<com.example.cookapp.api.dto.NutritionDto> nutRes = com.example.cookapp.api.RetrofitClient
                    .getClient(this).create(com.example.cookapp.api.ApiService.class)
                    .getRecipeNutrition(recipeId).execute();
                if (nutRes.isSuccessful() && nutRes.body() != null) {
                    db.nutritionDao().deleteNutritionFactByRecipe(recipeId);
                    com.example.cookapp.api.dto.NutritionDto dto = nutRes.body();
                    com.example.cookapp.data.local.entity.NutritionFactEntity nEntity = new com.example.cookapp.data.local.entity.NutritionFactEntity();
                    nEntity.recipe_id = recipeId;
                    nEntity.calories = (int) dto.calories;
                    nEntity.protein = dto.protein;
                    nEntity.fat = dto.fat;
                    nEntity.carbs = dto.carbs;
                    nEntity.fiber = dto.fiber;
                    nEntity.sugar = dto.sugar;
                    nEntity.sodium = dto.sodium;
                    db.nutritionDao().insertNutritionFact(nEntity);
                }
                
            } catch (Exception ignored) { }

            RecipeEntity recipe = db.recipeDao().getRecipeById(recipeId);
            List<RecipeStepEntity> steps = db.recipeDao().getStepsByRecipeId(recipeId);
            List<RecipeIngredientEntity> rawIngs = db.recipeDao().getIngredientsByRecipeId(recipeId);
            // Dedup: loại bỏ nguyên liệu trùng ingredient_id
            java.util.LinkedHashMap<Integer, RecipeIngredientEntity> dedupMap = new java.util.LinkedHashMap<>();
            if (rawIngs != null) {
                for (RecipeIngredientEntity ri : rawIngs) {
                    dedupMap.putIfAbsent(ri.ingredient_id, ri);
                }
            }
            List<RecipeIngredientEntity> ings = new ArrayList<>(dedupMap.values());
            NutritionFactEntity nutrition = db.nutritionDao().getNutritionByRecipe(recipeId);
            
            final double finalAvg = db.favoriteReviewDao().getAverageRating(recipeId);

            // Lấy tên nguyên liệu từ bảng ingredients trong Room
            Map<Integer, String> ingNames = new HashMap<>();
            List<com.example.cookapp.data.local.entity.IngredientEntity> allIngs =
                db.recipeDao().getAllIngredients();
            for (com.example.cookapp.data.local.entity.IngredientEntity ing : allIngs) {
                ingNames.put(ing.id, ing.name);
            }

            runOnUiThread(() -> {
                if (recipe != null) {
                    loadedRecipe = recipe;
                    loadedNutrition = nutrition;
                    baseServings = recipe.servings > 0 ? recipe.servings : 4;
                    currentServings = baseServings;

                    TextView tvTitle = findViewById(R.id.tv_title);
                    if (tvTitle != null) tvTitle.setText(recipe.title);

                    TextView tvAuthor = findViewById(R.id.tv_author);
                    if (tvAuthor != null) {
                        if (recipe.imageSource != null && !recipe.imageSource.trim().isEmpty()) {
                            tvAuthor.setText("Nguồn: " + recipe.imageSource);
                        } else {
                            tvAuthor.setText("Tác giả: CookApp Team");
                        }
                    }

                    ImageView ivRecipe = findViewById(R.id.iv_recipe);
                    if (ivRecipe != null) {
                        com.example.cookapp.utils.image.GlideHelper.loadRecipe(
                            RecipeDetailActivity.this, recipe.image_url, ivRecipe);
                    }

                    TextView tvDesc = findViewById(R.id.tv_description);
                    if (tvDesc != null) tvDesc.setText(recipe.description);

                    TextView tvServe = findViewById(R.id.tv_serve_count);
                    if (tvServe != null) tvServe.setText(String.valueOf(currentServings));

                    TextView tvTime = findViewById(R.id.tv_time);
                    if (tvTime != null) tvTime.setText(recipe.cook_time + " phút");

                    TextView tvDifficulty = findViewById(R.id.tv_difficulty);
                    if (tvDifficulty != null) tvDifficulty.setText(recipe.difficulty != null && !recipe.difficulty.isEmpty() ? recipe.difficulty : "Trung bình");

                    TextView tvRatingVal = findViewById(R.id.tv_rating_val);
                    // Hiển thị rating tạm từ Room trước
                    if (tvRatingVal != null) {
                        String ratingStr = (finalAvg > 0) ? String.format(java.util.Locale.US, "%.1f", finalAvg) : "0.0";
                        tvRatingVal.setText("Đánh giá: " + ratingStr + "/5 ⭐");
                    }
                    // Gọi API lấy rating realtime để cập nhật chính xác
                    fetchRealtimeRating(tvRatingVal);

                    // Video: phát inline — YouTube IFrame API qua WebView
                    View videoBlock = findViewById(R.id.video_block);
                    playerView = findViewById(R.id.player_view);
                    videoThumbnailContainer = findViewById(R.id.video_thumbnail_container);

                    if (recipe.video_url != null && !recipe.video_url.isEmpty()) {
                        if (videoBlock != null) videoBlock.setVisibility(View.VISIBLE);

                        // Load thumbnail cho video preview
                        ImageView ivMedia = findViewById(R.id.iv_media);
                        if (ivMedia != null) {
                            String thumbUrl;
                            if (recipe.video_thumbnail_url != null && !recipe.video_thumbnail_url.isEmpty()) {
                                thumbUrl = recipe.video_thumbnail_url;
                            } else {
                                thumbUrl = recipe.image_url;
                            }
                            Glide.with(RecipeDetailActivity.this).load(thumbUrl).into(ivMedia);
                        }

                        View btnPlayVideo = findViewById(R.id.btn_play_video);
                        final String finalVideoUrl = recipe.video_url;
                        if (btnPlayVideo != null) {
                            // Phát TRỰC TIẾP trên trang chi tiết (do MP4 đã được FFmpeg fix chuẩn)
                            btnPlayVideo.setOnClickListener(v -> playVideoInline(finalVideoUrl));
                        }
                    } else {
                        if (videoBlock != null) videoBlock.setVisibility(View.GONE);
                    }
                }

                // Lưu nguyên liệu gốc để scale
                if (ings != null) {
                    baseIngredients.clear();
                    baseIngredients.addAll(ings);
                    ingredientNames.clear();
                    ingredientNames.putAll(ingNames);
                    renderScaledIngredients();
                }

                // ── CÁC BƯỚC NẤU → ll_steps (phần "Các bước thực hiện") ──
                LinearLayout llSteps = findViewById(R.id.ll_steps);
                if (llSteps != null && steps != null && !steps.isEmpty()) {
                    llSteps.removeAllViews();
                    for (RecipeStepEntity step : steps) {
                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setPadding(0, 0, 0, 16);

                        // Số thứ tự bước trong vòng tròn
                        TextView tvNum = new TextView(this);
                        tvNum.setText(String.valueOf(step.step_number));
                        tvNum.setTextSize(18f);
                        tvNum.setTextColor(Color.BLACK);
                        tvNum.setGravity(Gravity.CENTER);
                        tvNum.setWidth(88);
                        tvNum.setHeight(88);
                        tvNum.setBackgroundResource(R.drawable.bg_circle_outline);

                        // Nội dung bước kèm timer
                        TextView tvInst = new TextView(this);
                        String timer = step.timer_seconds > 0
                            ? "\n⏱ " + (step.timer_seconds >= 60 ? step.timer_seconds / 60 + " phút" : step.timer_seconds + " giây")
                            : "";
                        String stepText = (step.title != null && !step.title.isEmpty()) ? step.title : step.instruction;
                        tvInst.setText(stepText + timer);
                        tvInst.setTextSize(14f);
                        tvInst.setTextColor(Color.parseColor("#333333"));
                        tvInst.setPadding(24, 12, 12, 12);
                        tvInst.setBackgroundResource(R.drawable.bg_grey_pill);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        lp.setMarginStart(24);
                        tvInst.setLayoutParams(lp);

                        row.addView(tvNum);
                        row.addView(tvInst);
                        llSteps.addView(row);
                    }
                }

                // ── NGUYÊN LIỆU → ll_ingredients (Tab "Nguyên liệu") ──
                if (llIngredients != null) {
                    llIngredients.removeAllViews();
                    if (ings != null && !ings.isEmpty()) {
                        for (RecipeIngredientEntity ing : ings) {
                            String name = ingNames.getOrDefault(ing.ingredient_id, "Nguyên liệu #" + ing.ingredient_id);
                            String qty = (ing.quantity == (int) ing.quantity)
                                ? (int) ing.quantity + " " + ing.unit
                                : ing.quantity + " " + ing.unit;

                            LinearLayout row = new LinearLayout(this);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setPadding(0, 16, 0, 16);

                            TextView tvName = new TextView(this);
                            tvName.setText(name);
                            tvName.setTextSize(15f);
                            tvName.setTextColor(Color.BLACK);
                            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                            tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                            TextView tvQty = new TextView(this);
                            tvQty.setText(qty);
                            tvQty.setTextSize(15f);
                            tvQty.setTextColor(Color.parseColor("#49454F"));
                            tvQty.setGravity(Gravity.END);

                            row.addView(tvName);
                            row.addView(tvQty);
                            llIngredients.addView(row);

                            View div = new View(this);
                            div.setBackgroundColor(Color.parseColor("#F0F0F0"));
                            div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                            llIngredients.addView(div);
                        }
                    }
                }

                // ── DANH SÁCH MUA SẮM → ll_shopping_items (kiểm tra siêu thị) ──
                LinearLayout llShoppingItems = findViewById(R.id.ll_shopping_items);
                if (llShoppingItems != null && ings != null && !ings.isEmpty()) {
                    llShoppingItems.removeAllViews();

                    // Thêm loading indicator
                    android.widget.TextView tvLoading = new android.widget.TextView(RecipeDetailActivity.this);
                    tvLoading.setText("🔍 Đang tìm sản phẩm tại siêu thị...");
                    tvLoading.setTextColor(android.graphics.Color.parseColor("#888888"));
                    tvLoading.setTextSize(13f);
                    tvLoading.setPadding(0, 8, 0, 8);
                    llShoppingItems.addView(tvLoading);

                    // Collect ingredient names
                    java.util.List<String> ingNameList = new java.util.ArrayList<>();
                    for (com.example.cookapp.data.local.entity.RecipeIngredientEntity ing : ings) {
                        String n = ingNames.getOrDefault(ing.ingredient_id, "");
                        if (!n.isEmpty()) ingNameList.add(n);
                    }

                    // Gọi API check-ingredients (chạy trên UI thread nhưng callback async)
                    com.example.cookapp.api.dto.CheckIngredientsRequest checkReq =
                        new com.example.cookapp.api.dto.CheckIngredientsRequest(ingNameList);

                    com.example.cookapp.api.RetrofitClient.getClient(RecipeDetailActivity.this)
                        .create(com.example.cookapp.api.ApiService.class)
                        .checkIngredients(checkReq)
                        .enqueue(new retrofit2.Callback<java.util.Map<String, java.util.List<com.example.cookapp.api.dto.StoreProductDto>>>() {
                            @Override
                            public void onResponse(
                                retrofit2.Call<java.util.Map<String, java.util.List<com.example.cookapp.api.dto.StoreProductDto>>> call,
                                retrofit2.Response<java.util.Map<String, java.util.List<com.example.cookapp.api.dto.StoreProductDto>>> response
                            ) {
                                java.util.Map<String, java.util.List<com.example.cookapp.api.dto.StoreProductDto>> storeMap =
                                    (response.isSuccessful() && response.body() != null)
                                    ? response.body()
                                    : new java.util.HashMap<>();

                                runOnUiThread(() -> {
                                    llShoppingItems.removeAllViews();
                                    renderShoppingWithStore(llShoppingItems, ings, ingNames, storeMap);
                                });
                            }

                            @Override
                            public void onFailure(retrofit2.Call<java.util.Map<String, java.util.List<com.example.cookapp.api.dto.StoreProductDto>>> call, Throwable t) {
                                // Fallback: hiển thị checkbox không có giá
                                runOnUiThread(() -> {
                                    llShoppingItems.removeAllViews();
                                    renderShoppingWithStore(llShoppingItems, ings, ingNames, new java.util.HashMap<>());
                                });
                            }
                        });
                }

                // Dinh dưỡng sẽ được render thông qua hàm renderScaledIngredients
                // Cùng lúc với setup ings ở block code trên.

            });
        });
        executor.shutdown();
    }

    /**
     * Phát video inline:
     * - Chỉ sử dụng ExoPlayer với AudioAttributes nghiêm ngặt để đảm bảo lấy trọn vẹn AudioFocus.
     * - Các video YouTube sẽ bắn thẳng sang YoutubeApp thay vì dùng WebView.
     */
    @androidx.media3.common.util.UnstableApi
    private void playVideoInline(String videoUrl) {
        // We NO LONGER hide the thumbnail here! We wait until STATE_READY to prevent black-screen buffering!
        
        if (playerView != null) {
            playerView.setVisibility(View.VISIBLE);
            playerView.setFullscreenButtonClickListener(isFullScreen -> toggleFullscreen());
            currentVideoUrl = videoUrl;
            
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }
            
            setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
            
            DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(25000, 50000, 1500, 5000)
                .build();
            
            androidx.media3.datasource.DataSource.Factory cachedDataSourceFactory = 
                VideoCacheManager.buildCachedDataSourceFactory(this);
            
            exoPlayer = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(
                    new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cachedDataSourceFactory)
                )
                .build();

            // AudioAttributes: xin Audio Focus tự động
            exoPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                false // Không để hệ thống tự ý tắt tiếng
            );

            // Volume set 1 lần duy nhất — KHÔNG trong listener
            exoPlayer.setVolume(1f);

            exoPlayer.addListener(new androidx.media3.common.Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == androidx.media3.common.Player.STATE_READY) {
                        if (videoThumbnailContainer != null) {
                            videoThumbnailContainer.setVisibility(View.GONE);
                        }
                        // Log cache trên background thread để không block audio
                        new Thread(() -> VideoCacheManager.logCacheStatus(RecipeDetailActivity.this)).start();
                    }
                }

                @Override
                public void onPositionDiscontinuity(
                    androidx.media3.common.Player.PositionInfo oldPos,
                    androidx.media3.common.Player.PositionInfo newPos,
                    int reason
                ) {
                    // Sau khi tua xong → force AudioTrack re-sync
                    if (reason == androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK) {
                        exoPlayer.setVolume(exoPlayer.getVolume());
                    }
                }

                @Override
                public void onPlayerError(androidx.media3.common.PlaybackException error) {
                    android.widget.Toast.makeText(RecipeDetailActivity.this, "Không thể tải video, vui lòng kiểm tra kết nối mạng!", android.widget.Toast.LENGTH_SHORT).show();
                    if (videoThumbnailContainer != null) {
                        videoThumbnailContainer.setVisibility(View.VISIBLE);
                    }
                }
            });

            playerView.setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS);
            playerView.setPlayer(exoPlayer);
            MediaItem standardMediaItem = MediaItem.fromUri(videoUrl);
            exoPlayer.setMediaItem(standardMediaItem);
            exoPlayer.prepare();
            exoPlayer.setPlayWhenReady(true);
        }
    }
    
    private android.app.Dialog fullscreenDialog;
    private boolean isFullscreen = false;
    private int originalVideoBlockIndex = -1;
    private String currentVideoUrl = null;
    private long savedVideoPosition = 0;
    private boolean wasPlayingBeforePause = true;

    private void toggleFullscreen() {
        FrameLayout videoBlock = findViewById(R.id.video_block);
        if (isFullscreen) {
            if (fullscreenDialog != null) fullscreenDialog.dismiss();
            // isFullscreen = false is handled in the OnDismissListener
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            if (videoBlock == null) return;
            
            // Extract from standard layout
            if (videoBlock.getParent() instanceof LinearLayout) {
                LinearLayout contentContainer = (LinearLayout) videoBlock.getParent();
                originalVideoBlockIndex = contentContainer.indexOfChild(videoBlock);
                contentContainer.removeView(videoBlock);
            } else {
                ((ViewGroup) videoBlock.getParent()).removeView(videoBlock);
            }
            
            // Prepare dialog if needed
            if (fullscreenDialog == null) {
                fullscreenDialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                fullscreenDialog.setOnDismissListener(dialog -> {
                    isFullscreen = false;
                    
                    // Extract videoBlock from dialog using the known ID
                    FrameLayout activeVideoBlock = fullscreenDialog.findViewById(R.id.video_block);
                    if (activeVideoBlock == null) return; // Should not happen
                    
                    ViewGroup dialogParent = (ViewGroup) activeVideoBlock.getParent();
                    if (dialogParent != null) dialogParent.removeView(activeVideoBlock);
                    
                    // Restore to original location
                    View referenceView = findViewById(R.id.tv_description);
                    if (referenceView != null && referenceView.getParent() instanceof LinearLayout) {
                        LinearLayout contentContainer = (LinearLayout) referenceView.getParent();
                        if (originalVideoBlockIndex != -1) {
                            activeVideoBlock.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 
                                (int)(220 * getResources().getDisplayMetrics().density)));
                            
                            // MarginTop 16dp
                            ((LinearLayout.LayoutParams) activeVideoBlock.getLayoutParams()).topMargin = 
                                (int)(16 * getResources().getDisplayMetrics().density);
                                
                            contentContainer.addView(activeVideoBlock, originalVideoBlockIndex);
                        }
                    }
                });
            }
            
            // Add to dialog and show
            videoBlock.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            fullscreenDialog.setContentView(videoBlock);
            fullscreenDialog.show();
            
            // True immersive fullscreen for dialog
            if (fullscreenDialog.getWindow() != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    android.view.WindowInsetsController insetsController = 
                        fullscreenDialog.getWindow().getInsetsController();
                    if (insetsController != null) {
                        insetsController.hide(android.view.WindowInsets.Type.systemBars());
                        insetsController.setSystemBarsBehavior(
                            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    }
                } else {
                    fullscreenDialog.getWindow().getDecorView().setSystemUiVisibility(
                          View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
            isFullscreen = true;
            
            // Force landscape
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) {
            wasPlayingBeforePause = exoPlayer.getPlayWhenReady();
            exoPlayer.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(wasPlayingBeforePause);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playerView != null) {
            playerView.setPlayer(null);
        }
        if (exoPlayer != null) { 
            exoPlayer.release(); 
            exoPlayer = null; 
        }
    }

    /**
     * Render danh sách nguyên liệu kèm thông tin siêu thị có bán.
     * - Nếu siêu thị có sản phẩm phù hợp: hiện badge tên store + giá → checkbox enabled
     * - Nếu không có: hiện badge "Không tìm thấy" → checkbox disabled (không cho add giỏ)
     */
    private void renderShoppingWithStore(
        LinearLayout container,
        java.util.List<com.example.cookapp.data.local.entity.RecipeIngredientEntity> ings,
        java.util.Map<Integer, String> ingNames,
        java.util.Map<String, java.util.List<com.example.cookapp.api.dto.StoreProductDto>> storeMap
    ) {
        container.removeAllViews();

        // Dedup: tránh hiển thị trùng nguyên liệu
        java.util.Set<Integer> renderedIds = new java.util.HashSet<>();

        for (com.example.cookapp.data.local.entity.RecipeIngredientEntity ing : ings) {
            // Bỏ qua nếu nguyên liệu đã render
            if (!renderedIds.add(ing.ingredient_id)) continue;

            String name = ingNames.getOrDefault(ing.ingredient_id, "Nguyên liệu #" + ing.ingredient_id);
            String qtyStr = (ing.quantity == (int) ing.quantity)
                ? String.valueOf((int) ing.quantity)
                : String.valueOf(ing.quantity);
            String unit = ing.unit != null ? ing.unit : "";

            // Tìm sản phẩm rẻ nhất trong store map
            java.util.List<com.example.cookapp.api.dto.StoreProductDto> products = storeMap.get(name);
            boolean hasStore = products != null && !products.isEmpty();
            com.example.cookapp.api.dto.StoreProductDto cheapest = hasStore ? products.get(0) : null;

            // ── Row container ──
            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.VERTICAL);
            row.setPadding(0, 10, 0, 10);
            row.setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

            // ── Hàng 1: Checkbox tên nguyên liệu ──
            android.widget.CheckBox cb = new android.widget.CheckBox(this);
            String displayQty = hasStore ? "1" : qtyStr;
            String displayUnit = hasStore && cheapest.unit != null ? cheapest.unit : unit;
            cb.setText(name + "  (" + displayQty + (hasStore ? " x " : " ") + displayUnit + ")");
            cb.setTextColor(hasStore ? android.graphics.Color.parseColor("#1A1A1A") : android.graphics.Color.parseColor("#AAAAAA"));
            cb.setTextSize(14f);
            cb.setChecked(hasStore);              // chỉ check nếu có sản phẩm
            cb.setEnabled(hasStore);              // disable nếu không tìm thấy ở siêu thị
            // Tag: "productId|name|qty|unit|priceDong|storeProductId"
            int prodId = cheapest != null ? cheapest.id : -1;
            int price  = cheapest != null ? cheapest.priceDong : 0;
            String storeUnit = cheapest != null && cheapest.unit != null ? cheapest.unit : unit;
            cb.setTag(ing.ingredient_id + "|" + name + "|" + displayQty + "|" + storeUnit + "|" + price + "|" + prodId);
            row.addView(cb);

            // ── Hàng 2: Store badge ──
            android.widget.TextView tvStore = new android.widget.TextView(this);
            tvStore.setTextSize(12f);
            tvStore.setPadding(32, 2, 0, 4);

            if (hasStore && cheapest != null) {
                String priceStr = formatVndDetail(cheapest.priceDong);
                String stars = cheapest.rating >= 4.5 ? "⭐" : "";
                tvStore.setText("🛒 " + cheapest.storeName + "  ·  " + cheapest.unit
                    + "  ·  " + priceStr + " đ  " + stars);
                tvStore.setTextColor(android.graphics.Color.parseColor("#FF7043"));
            } else {
                tvStore.setText("⚠ Chưa tìm thấy ở WinMart");
                tvStore.setTextColor(android.graphics.Color.parseColor("#999999"));
            }
            row.addView(tvStore);

            // Divider
            android.view.View div = new android.view.View(this);
            div.setBackgroundColor(android.graphics.Color.parseColor("#F0F0F0"));
            div.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
            row.addView(div);

            container.addView(row);
        }
    }

    /** Format giá VND dạng 45.000 */
    private String formatVndDetail(int dong) {
        if (dong <= 0) return "0";
        java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,###");
        return fmt.format(dong).replace(",", ".");
    }

    /** Tạo một hàng dinh dưỡng (label - value) trong llNutrition */
    private void addNutritionRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 14, 0, 14);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(14f);
        tvLabel.setTextColor(Color.parseColor("#555555"));
        LinearLayout.LayoutParams lpLabel = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvLabel.setLayoutParams(lpLabel);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(14f);
        tvValue.setTextColor(Color.parseColor("#FF6B35"));
        tvValue.setGravity(Gravity.END);
        LinearLayout.LayoutParams lpValue = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvValue.setLayoutParams(lpValue);

        row.addView(tvLabel);
        row.addView(tvValue);
        parent.addView(row);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#F0F0F0"));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divider.setLayoutParams(dp);
        parent.addView(divider);
    }

    /** Chia sẻ công thức nấu ăn */
    private void shareRecipe() {
        String shareText = "Hãy thử nấu món ăn tuyệt vời này cùng CookApp!";
        if (loadedRecipe != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("🍳 ").append(loadedRecipe.title != null ? loadedRecipe.title : "Công thức mới").append("\n\n");
            if (loadedRecipe.description != null && !loadedRecipe.description.isEmpty()) {
                sb.append("📝 ").append(loadedRecipe.description).append("\n");
            }
            sb.append("⏳ Thời gian nấu: ").append(loadedRecipe.cook_time > 0 ? loadedRecipe.cook_time + " phút" : "Tùy biến").append("\n");
            sb.append("🍽 Khẩu phần: ").append(loadedRecipe.servings > 0 ? loadedRecipe.servings + " người" : "Không xác định").append("\n\n");
            sb.append("Tải ngay ứng dụng CookApp để xem chi tiết cách làm nhé!");
            shareText = sb.toString();
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ công thức qua..."));
    }

    /** Render ingredients scaled to currentServings/baseServings ratio */
    private void renderScaledIngredients() {
        if (llIngredients == null) return;
        llIngredients.removeAllViews();
        float ratio = (baseServings > 0) ? (float) currentServings / baseServings : 1f;
        for (RecipeIngredientEntity ing : baseIngredients) {
            String name = ingredientNames.getOrDefault(ing.ingredient_id, "Nguyen lieu #" + ing.ingredient_id);
            float scaledQty = ing.quantity * ratio;
            String qtyStr = (scaledQty == (int) scaledQty)
                ? (int) scaledQty + " " + ing.unit
                : String.format(java.util.Locale.getDefault(), "%.1f", scaledQty) + " " + ing.unit;

            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setPadding(0, 16, 0, 16);

            android.widget.TextView tvName = new android.widget.TextView(this);
            tvName.setText(name);
            tvName.setTextSize(15f);
            tvName.setTextColor(android.graphics.Color.BLACK);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvName.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            android.widget.TextView tvQty = new android.widget.TextView(this);
            tvQty.setText(qtyStr);
            tvQty.setTextSize(15f);
            tvQty.setTextColor(android.graphics.Color.parseColor("#49454F"));
            tvQty.setGravity(android.view.Gravity.END);

            row.addView(tvName);
            row.addView(tvQty);
            llIngredients.addView(row);

            android.view.View div = new android.view.View(this);
            div.setBackgroundColor(android.graphics.Color.parseColor("#F0F0F0"));
            div.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1));
            llIngredients.addView(div);
        }

        // Scale Nutrition
        LinearLayout llNutrition = findViewById(R.id.ll_nutrition);
        if (llNutrition != null && loadedNutrition != null) {
            llNutrition.removeAllViews();
            addNutritionRow(llNutrition, "🔥 Calories",  (int)(loadedNutrition.calories * ratio) + " kcal");
            addNutritionRow(llNutrition, "🥩 Protein",   String.format(java.util.Locale.US, "%.1f", loadedNutrition.protein * ratio) + " g");
            addNutritionRow(llNutrition, "🧈 Chất béo",  String.format(java.util.Locale.US, "%.1f", loadedNutrition.fat * ratio) + " g");
            addNutritionRow(llNutrition, "🍞 Carbs",     String.format(java.util.Locale.US, "%.1f", loadedNutrition.carbs * ratio) + " g");
            addNutritionRow(llNutrition, "🌾 Chất xơ",   String.format(java.util.Locale.US, "%.1f", loadedNutrition.fiber * ratio) + " g");
            addNutritionRow(llNutrition, "🍬 Đường",     String.format(java.util.Locale.US, "%.1f", loadedNutrition.sugar * ratio) + " g");
            addNutritionRow(llNutrition, "🧂 Natri",     (int)(loadedNutrition.sodium * ratio) + " mg");
        }
    }

    /**
     * Gọi API /api/recipes/:id/reviews để lấy average_rating realtime
     * → cập nhật lên tvRatingVal cho khớp với trang ReviewActivity
     */
    private void fetchRealtimeRating(TextView tvRatingVal) {
        if (tvRatingVal == null || recipeId <= 0) return;
        try {
            com.example.cookapp.api.ApiService api =
                com.example.cookapp.api.RetrofitClient.getClient(this)
                    .create(com.example.cookapp.api.ApiService.class);

            api.getRecipeReviews(recipeId).enqueue(new retrofit2.Callback<com.example.cookapp.api.dto.ReviewsResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.cookapp.api.dto.ReviewsResponse> call,
                                        retrofit2.Response<com.example.cookapp.api.dto.ReviewsResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        com.example.cookapp.api.dto.ReviewsResponse body = response.body();
                        runOnUiThread(() -> {
                            String ratingStr = body.count > 0
                                ? String.format(java.util.Locale.US, "%.1f", body.average_rating)
                                : "0.0";
                            tvRatingVal.setText("Đánh giá: " + ratingStr + "/5 ⭐");
                        });
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.cookapp.api.dto.ReviewsResponse> call, Throwable t) {
                    // Giữ nguyên rating từ Room nếu API lỗi — không cần xử lý
                }
            });
        } catch (Exception e) {
            // Bỏ qua lỗi — giữ rating từ Room
        }
    }
}
