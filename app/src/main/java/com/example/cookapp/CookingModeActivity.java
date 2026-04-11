package com.example.cookapp;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.ui.PlayerView;
import com.bumptech.glide.Glide;
import com.example.cookapp.data.local.AppDatabase;
import com.example.cookapp.data.local.entity.RecipeEntity;
import com.example.cookapp.data.local.entity.RecipeStepEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CookingModeActivity extends AppCompatActivity {

    // ── Views ────────────────────────────────────────────────────────────
    private TextView  tvStepIndicator;
    private TextView  btnPrev, btnNext;          // orange pill TextViews
    private TextView  tvTimer, tvTimerAction;
    private TextView  btnTimerReset;             // ↺ circle TextView
    private LinearLayout btnTimerToggle;
    private androidx.cardview.widget.CardView cvTimerContainer;
    private TextView  tvStepHeader, tvStepInstructions;

    // ── Data ─────────────────────────────────────────────────────────────
    private final List<RecipeStepEntity> steps = new ArrayList<>();
    private int currentIndex = 0;
    private String videoUrl = null;

    // ── Video Player ─────────────────────────────────────────────────────
    private ExoPlayer player;
    private PlayerView playerView;
    private ImageView ivStepImage;
    private boolean isFullscreen = false;
    private boolean wasPlayingBeforePause = true;

    // ── Timer ────────────────────────────────────────────────────────────
    private CountDownTimer activeTimer;
    private int  timerSeconds    = 0;
    private int  remainingSeconds = 0;
    private boolean timerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Add Keep Screen On
        setContentView(R.layout.activity_cooking_mode);

        // Bind views
        tvStepIndicator    = findViewById(R.id.tv_step_indicator);
        btnPrev            = findViewById(R.id.btn_prev);
        btnNext            = findViewById(R.id.btn_next);
        tvTimer            = findViewById(R.id.tv_timer);
        tvTimerAction      = findViewById(R.id.tv_timer_action);
        btnTimerToggle     = findViewById(R.id.btn_timer_toggle);
        btnTimerReset      = findViewById(R.id.btn_timer_reset);
        cvTimerContainer   = findViewById(R.id.cv_timer_container);
        tvStepHeader       = findViewById(R.id.tv_step_header);
        tvStepInstructions = findViewById(R.id.tv_step_instructions);
        playerView         = findViewById(R.id.player_view);
        ivStepImage        = findViewById(R.id.iv_step_image);

        // Back → exit
        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> { cancelTimer(); finish(); });

        // Header nav pills
        if (btnPrev != null) btnPrev.setOnClickListener(v -> navigate(-1));
        if (btnNext != null) btnNext.setOnClickListener(v -> navigate(+1));

        // Timer
        if (btnTimerToggle != null) btnTimerToggle.setOnClickListener(v -> toggleTimer());
        if (btnTimerReset  != null) btnTimerReset.setOnClickListener(v -> resetTimer());

        // Load steps from DB
        int recipeId = getIntent().getIntExtra("recipe_id", -1);
        if (recipeId == -1) {
            Toast.makeText(this, "Không tìm thấy công thức", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadSteps(recipeId);
    }

    // ─────────────────────────────────────────────────────────────────────
    private void loadSteps(int recipeId) {
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.execute(() -> {
            RecipeEntity recipe = AppDatabase.getDatabase(this).recipeDao().getRecipeById(recipeId);
            List<RecipeStepEntity> loaded =
                AppDatabase.getDatabase(this).recipeDao().getStepsByRecipeId(recipeId);
            
            runOnUiThread(() -> {
                if (recipe != null) {
                    if (recipe.video_url != null && !recipe.video_url.isEmpty()) {
                        videoUrl = recipe.video_url;
                        initializePlayer(videoUrl);
                    } else if (recipe.image_url != null && !recipe.image_url.isEmpty()) {
                        if (playerView != null) playerView.setVisibility(View.GONE);
                        if (ivStepImage != null) {
                            ivStepImage.setVisibility(View.VISIBLE);
                            Glide.with(this).load(recipe.image_url).into(ivStepImage);
                        }
                    }
                }

                // Kiểm tra xem dữ liệu cache có hợp lệ không (có video_start_time > 0 không)
                boolean needsSync = true;
                if (loaded != null && !loaded.isEmpty()) {
                    for (RecipeStepEntity s : loaded) {
                        if (s.video_start_time > 0) {
                            needsSync = false;
                            break;
                        }
                    }
                }

                if (!needsSync) {
                    // Cache tốt — dùng ngay
                    steps.clear();
                    steps.addAll(loaded);
                    displayStep(0);
                } else {
                    // Cache thiếu video_start_time hoặc rỗng → tải mới từ API
                    loadStepsFromApi(recipeId);
                }
            });
        });
    }

    private void loadStepsFromApi(int recipeId) {
        com.example.cookapp.api.ApiService apiService =
            com.example.cookapp.api.RetrofitClient.getClient(this)
                .create(com.example.cookapp.api.ApiService.class);

        apiService.getRecipeSteps(recipeId).enqueue(new retrofit2.Callback<List<com.example.cookapp.api.dto.RecipeStepDto>>() {
            @Override
            public void onResponse(retrofit2.Call<List<com.example.cookapp.api.dto.RecipeStepDto>> call,
                                   retrofit2.Response<List<com.example.cookapp.api.dto.RecipeStepDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<RecipeStepEntity> apiSteps = new java.util.ArrayList<>();
                    for (com.example.cookapp.api.dto.RecipeStepDto dto : response.body()) {
                        RecipeStepEntity e = new RecipeStepEntity();
                        e.recipe_id    = recipeId;
                        e.step_number  = dto.step_number;
                        e.instruction  = dto.instruction;
                        e.timer_seconds = dto.timer_seconds;
                        e.video_start_time = dto.video_start_time;
                        apiSteps.add(e);
                    }

                    // ✅ LƯU VÀO ROOM DATABASE ĐỂ CACHE CHO LẦN SAU
                    Executors.newSingleThreadExecutor().execute(() -> {
                        AppDatabase db = AppDatabase.getDatabase(CookingModeActivity.this);
                        db.recipeDao().deleteStepsByRecipe(recipeId);
                        db.recipeDao().insertSteps(apiSteps);
                        
                        runOnUiThread(() -> {
                            steps.clear();
                            steps.addAll(apiSteps);
                            displayStep(0);
                        });
                    });

                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(CookingModeActivity.this,
                            "Công thức chưa có bước nấu", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<com.example.cookapp.api.dto.RecipeStepDto>> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(CookingModeActivity.this,
                        "Không tải được bước nấu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }


    @androidx.media3.common.util.UnstableApi
    private void initializePlayer(String url) {
        if (playerView != null) {
            playerView.setVisibility(View.VISIBLE);
            playerView.setFullscreenButtonClickListener(isFullScreen -> toggleFullscreen());
        }
        if (ivStepImage != null) ivStepImage.setVisibility(View.GONE);
        
        if (player != null) {
            player.release();
            player = null;
        }
        
        // Force audio to STREAM_MUSIC
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
            .setBufferDurationsMs(25000, 50000, 1500, 5000)
            .build();
        
        androidx.media3.datasource.DataSource.Factory cachedDataSourceFactory = 
            VideoCacheManager.buildCachedDataSourceFactory(this);
        
        player = new ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(
                new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cachedDataSourceFactory)
            )
            .build();

        // Audio Focus
        player.setAudioAttributes(
            new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            false // Không để hệ thống tự ý tắt tiếng
        );

        // Volume set 1 lần duy nhất — KHÔNG gọi lặp lại trong listener
        player.setVolume(1f);

        player.addListener(new androidx.media3.common.Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == androidx.media3.common.Player.STATE_READY) {
                    new Thread(() -> VideoCacheManager.logCacheStatus(CookingModeActivity.this)).start();
                }
            }

            @Override
            public void onPositionDiscontinuity(
                androidx.media3.common.Player.PositionInfo oldPos,
                androidx.media3.common.Player.PositionInfo newPos,
                int reason
            ) {
                // Sau khi tua hoặc loop lại đầu → force AudioTrack re-sync
                if (reason == androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
                    || reason == androidx.media3.common.Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    player.setVolume(player.getVolume());
                }
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                Toast.makeText(CookingModeActivity.this, "Không thể tải video bước này!", Toast.LENGTH_SHORT).show();
            }
        });

        player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
        if (playerView != null) {
            playerView.setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS);
            playerView.setPlayer(player);
        }
    }

    private void navigate(int delta) {
        int next = currentIndex + delta;
        if (next < 0 || next >= steps.size()) return;
        cancelTimer();
        currentIndex = next;
        displayStep(currentIndex);
    }

    // ─────────────────────────────────────────────────────────────────────
    private void displayStep(int index) {
        RecipeStepEntity step = steps.get(index);
        int total = steps.size();

        if (tvStepIndicator != null)
            tvStepIndicator.setText("Bước " + (index + 1) + "/" + total);
        if (tvStepHeader != null) {
            String headerText = (step.title != null && !step.title.isEmpty()) ? step.title : "Hướng dẫn bước " + (index + 1);
            tvStepHeader.setText(headerText);
        }
        if (tvStepInstructions != null)
            tvStepInstructions.setText(step.instruction);

        // Timer
        timerSeconds     = step.timer_seconds;
        remainingSeconds = timerSeconds;
        resetTimerDisplay();
        
        if (cvTimerContainer != null) {
            cvTimerContainer.setVisibility(timerSeconds > 0 ? View.VISIBLE : View.GONE);
        }

        // Nav buttons visibility
        boolean isFirst = (index == 0);
        boolean isLast  = (index == total - 1);
        if (btnPrev != null) btnPrev.setVisibility(isFirst ? View.GONE : View.VISIBLE);
        if (btnNext != null) {
            if (isLast) {
                btnNext.setText("✓ Xong");
                btnNext.setOnClickListener(v -> {
                    cancelTimer();
                    Toast.makeText(this, "Chúc mừng! Món ăn đã hoàn thành 🎉", Toast.LENGTH_LONG).show();
                    finish();
                });
            } else {
                btnNext.setText("Sau");
                btnNext.setOnClickListener(v -> navigate(+1));
            }
        }

        // Seek video to corresponding part using ClippingMediaSource
        if (player != null && videoUrl != null) {
            long startMs = step.video_start_time > 0 ? step.video_start_time * 1000L : 0;
            long endMs = 0;
            
            if (index + 1 < total) {
                RecipeStepEntity nextStep = steps.get(index + 1);
                endMs = nextStep.video_start_time > 0 ? nextStep.video_start_time * 1000L : 0;
            }

            MediaItem.ClippingConfiguration clippingConfig = new MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs > 0 ? endMs : C.TIME_END_OF_SOURCE)
                .build();
                
            MediaItem clippedMediaItem = new MediaItem.Builder()
                .setUri(videoUrl)
                .setClippingConfiguration(clippingConfig)
                .build();

            player.setMediaItem(clippedMediaItem, true);
            player.prepare();
            player.setPlayWhenReady(true);
        }
    }

    private void toggleFullscreen() {
        View header = findViewById(R.id.header);
        View instructionsCard = (View) findViewById(R.id.tv_step_header).getParent().getParent(); // CardView wrapper
        View timerCard = (View) findViewById(R.id.tv_timer).getParent().getParent().getParent(); // CardView wrapper
        androidx.cardview.widget.CardView cvVideo = findViewById(R.id.cv_video_container);

        isFullscreen = !isFullscreen;

        if (isFullscreen) {
            // Enter Fullscreen
            if (header != null) header.setVisibility(View.GONE);
            if (instructionsCard instanceof View) ((View) instructionsCard).setVisibility(View.GONE);
            if (timerCard instanceof View) ((View) timerCard).setVisibility(View.GONE);

            // Expand CardView to take full screen
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            params.setMargins(0, 0, 0, 0); // Remove margins
            cvVideo.setLayoutParams(params);
            cvVideo.setRadius(0); // Remove rounded corners in fullscreen
            
            // True immersive fullscreen
            getWindow().getDecorView().setSystemUiVisibility(
                  View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            
            // Force landscape
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            // Exit Fullscreen
            if (header != null) header.setVisibility(View.VISIBLE);
            if (instructionsCard instanceof View) ((View) instructionsCard).setVisibility(View.VISIBLE);
            if (timerCard instanceof View) ((View) timerCard).setVisibility(View.VISIBLE);

            // Restore CardView size
            int heightPx = (int) (240 * getResources().getDisplayMetrics().density); // Restore 240dp height
            int marginPx = (int) (16 * getResources().getDisplayMetrics().density); // Restore 16dp margin
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, heightPx);
            params.setMargins(marginPx, marginPx, marginPx, marginPx);
            cvVideo.setLayoutParams(params);
            cvVideo.setRadius(16 * getResources().getDisplayMetrics().density); // Restore rounded corners
            
            // Restore System UI
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            
            // Restore portrait
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }


    // ─────────────────────────────────────────────────────────────────────
    private void toggleTimer() {
        if (timerSeconds <= 0) {
            Toast.makeText(this, "Bước này không cần hẹn giờ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!timerRunning) startTimer(); else pauseTimer();
    }

    private void startTimer() {
        timerRunning = true;
        if (tvTimerAction != null) tvTimerAction.setText("Dừng");
        if (btnTimerToggle != null) btnTimerToggle.setBackgroundColor(Color.parseColor("#888888"));

        activeTimer = new CountDownTimer(remainingSeconds * 1000L, 1000) {
            @Override public void onTick(long ms) {
                remainingSeconds = (int)(ms / 1000);
                updateTimerDisplay(remainingSeconds);
            }
            @Override public void onFinish() {
                timerRunning = false;
                remainingSeconds = 0;
                updateTimerDisplay(0);
                if (tvTimerAction != null) tvTimerAction.setText("Bắt đầu");
                if (btnTimerToggle != null)
                    btnTimerToggle.setBackgroundColor(Color.parseColor("#4CAF50"));
                new AlertDialog.Builder(CookingModeActivity.this)
                    .setTitle("⏰ Hết giờ!")
                    .setMessage("Bước " + (currentIndex + 1) + " đã hoàn thành!")
                    .setPositiveButton("OK", null).show();
            }
        }.start();
    }

    private void pauseTimer() {
        timerRunning = false;
        cancelTimer();
        if (tvTimerAction != null) tvTimerAction.setText("Tiếp tục");
        if (btnTimerToggle != null)
            btnTimerToggle.setBackgroundColor(Color.parseColor("#CC0000"));
    }

    private void resetTimer() {
        cancelTimer();
        remainingSeconds = timerSeconds;
        resetTimerDisplay();
    }

    private void cancelTimer() {
        timerRunning = false;
        if (activeTimer != null) { activeTimer.cancel(); activeTimer = null; }
    }

    private void resetTimerDisplay() {
        updateTimerDisplay(timerSeconds);
        if (tvTimerAction != null) tvTimerAction.setText("Bắt đầu");
        if (btnTimerToggle != null)
            btnTimerToggle.setBackgroundColor(Color.parseColor("#CC0000"));
        if (tvTimer != null) tvTimer.setTextColor(Color.BLACK);
    }

    private void updateTimerDisplay(int seconds) {
        if (tvTimer == null) return;
        int h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        tvTimer.setText(h > 0
            ? String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
            : String.format(Locale.getDefault(), "%02d:%02d", m, s));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            wasPlayingBeforePause = player.getPlayWhenReady();
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(wasPlayingBeforePause);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
        if (playerView != null) {
            playerView.setPlayer(null);
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }
    
    @Override
    public void onBackPressed() {
        if (isFullscreen) {
            toggleFullscreen();
            return;
        }
        super.onBackPressed();
    }
}
