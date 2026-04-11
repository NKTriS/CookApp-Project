package com.example.cookapp;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/**
 * Lightweight Activity for video playback — same pattern as CookingModeActivity.
 * Opens in landscape fullscreen with ExoPlayer controls.
 */
public class VideoPlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;

    @androidx.media3.common.util.UnstableApi
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Fullscreen landscape — like YouTube
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        // Simple layout: just a PlayerView
        playerView = new PlayerView(this);
        playerView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(playerView);
        
        // Immersive fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        
        // Get video URL from intent
        String videoUrl = getIntent().getStringExtra("video_url");
        if (videoUrl == null || videoUrl.isEmpty()) {
            finish();
            return;
        }
        
        // ═══ EXACT SAME code as CookingModeActivity.initializePlayer() ═══
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
            .setBufferDurationsMs(5000, 15000, 2500, 5000)
            .build();
        
        androidx.media3.common.AudioAttributes audioAttributes = new androidx.media3.common.AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build();

        player = new ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .build();
        player.setVolume(1.0f);
        playerView.setPlayer(player);
        
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        
        player.addListener(new androidx.media3.common.Player.Listener() {
            @Override
            public void onPositionDiscontinuity(
                    androidx.media3.common.Player.PositionInfo oldPosition,
                    androidx.media3.common.Player.PositionInfo newPosition,
                    int reason) {
                if (reason == androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK) {
                    // CÁCH TRIỆT ĐỂ 100%: 
                    // Khi thanh tua được nhả ra, phần cứng Android bị crash audio. 
                    // Ta KHÔNG báo lỗi, mà lập tức ép Player "prepare" (khởi động) lại toàn bộ bộ giải mã
                    // từ đúng vị trí vừa tua tới! Thao tác này mất ~0.1s và sẽ gõ thức AudioTrack.
                    player.prepare();
                    player.setVolume(1.0f);
                }
            }
        });
        
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setVolume(1.0f);
        player.setPlayWhenReady(true);
        
        // Back button → close
        playerView.setFullscreenButtonClickListener(isFullScreen -> finish());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setVolume(1.0f);
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
