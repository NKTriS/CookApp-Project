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
 * Màn hình phát video nấu ăn độc lập.
 *
 * Activity này nhận video_url từ Intent, mở trình phát ở chế độ ngang toàn màn hình
 * và dùng ExoPlayer để phát video hướng dẫn. Màn hình này phục vụ chức năng
 * "Video nấu ăn" khi người dùng muốn xem video đầy đủ ngoài chế độ Cooking Mode.
 */
public class VideoPlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;

    @androidx.media3.common.util.UnstableApi
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ép toàn màn hình và xoay ngang để trải nghiệm xem video giống trình phát chuyên dụng.
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        // Tạo PlayerView bằng code vì màn hình này chỉ cần một trình phát video toàn màn hình.
        playerView = new PlayerView(this);
        playerView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(playerView);
        
        // Ẩn thanh trạng thái và thanh điều hướng Android để video chiếm trọn màn hình.
        getWindow().getDecorView().setSystemUiVisibility(
              View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        
        // Lấy đường dẫn video từ Intent; nếu thiếu URL thì đóng màn hình để tránh player rỗng.
        String videoUrl = getIntent().getStringExtra("video_url");
        if (videoUrl == null || videoUrl.isEmpty()) {
            finish();
            return;
        }
        
        // Cấu hình luồng âm thanh media và bộ đệm phát video cho ExoPlayer.
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
        
        // MediaItem là đối tượng đại diện cho URL video mà ExoPlayer sẽ tải và phát.
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        
        player.addListener(new androidx.media3.common.Player.Listener() {
            @Override
            public void onPositionDiscontinuity(
                    androidx.media3.common.Player.PositionInfo oldPosition,
                    androidx.media3.common.Player.PositionInfo newPosition,
                    int reason) {
                if (reason == androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK) {
                    // Sau thao tác tua, prepare lại player để đồng bộ bộ giải mã âm thanh/video.
                    player.prepare();
                    player.setVolume(1.0f);
                }
            }
        });
        
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setVolume(1.0f);
        player.setPlayWhenReady(true);
        
        // Nút fullscreen/back trên PlayerView được dùng như nút đóng màn hình video.
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
