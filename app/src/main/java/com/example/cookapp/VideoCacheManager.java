package com.example.cookapp;

import android.content.Context;
import android.util.Log;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import java.io.File;

/**
 * Singleton quản lý Video Cache cho ExoPlayer.
 * - Lần đầu stream → lưu vào cache trên ổ đĩa.
 * - Các lần sau → phát trực tiếp từ cache, mượt như video local.
 * - Tự động xóa video cũ nhất khi cache đầy (LRU).
 */
@UnstableApi
public class VideoCacheManager {

    private static final String TAG = "VideoCacheManager";
    private static final long MAX_CACHE_SIZE = 512 * 1024 * 1024; // 512 MB
    private static SimpleCache simpleCache;

    private VideoCacheManager() {}

    /**
     * Lấy instance SimpleCache duy nhất cho toàn app.
     * Thread-safe với synchronized.
     */
    public static synchronized SimpleCache getCache(Context context) {
        if (simpleCache == null) {
            File cacheDir = new File(context.getCacheDir(), "video_cache");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            simpleCache = new SimpleCache(
                cacheDir,
                new LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE),
                new StandaloneDatabaseProvider(context)
            );
            Log.d(TAG, "✅ Cache khởi tạo: " + cacheDir.getAbsolutePath());
        }
        return simpleCache;
    }

    /**
     * Tạo DataSource.Factory có hỗ trợ cache.
     */
    public static DataSource.Factory buildCachedDataSourceFactory(Context context) {
        SimpleCache cache = getCache(context);
        DataSource.Factory upstreamFactory = new DefaultDataSource.Factory(context);
        return new CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    /**
     * Log thông tin cache hiện tại (gọi sau khi video đã phát).
     * Mở Logcat → lọc "VideoCacheManager" để xem.
     */
    public static void logCacheStatus(Context context) {
        SimpleCache cache = getCache(context);
        long cachedBytes = cache.getCacheSpace();
        int cachedKeys = cache.getKeys().size();
        
        double cachedMB = cachedBytes / (1024.0 * 1024.0);
        double maxMB = MAX_CACHE_SIZE / (1024.0 * 1024.0);
        
        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "📦 VIDEO CACHE STATUS");
        Log.d(TAG, "───────────────────────────────────────");
        Log.d(TAG, "📁 Số video đã cache: " + cachedKeys);
        Log.d(TAG, "💾 Dung lượng cache : " + String.format("%.1f MB / %.0f MB", cachedMB, maxMB));
        
        for (String key : cache.getKeys()) {
            long keyBytes = 0;
            for (androidx.media3.datasource.cache.CacheSpan span : cache.getCachedSpans(key)) {
                keyBytes += span.length;
            }
            double keyMB = keyBytes / (1024.0 * 1024.0);
            Log.d(TAG, "  🎬 " + key.substring(0, Math.min(key.length(), 60)) + "... → " + String.format("%.1f MB", keyMB));
        }
        
        Log.d(TAG, "═══════════════════════════════════════");
    }

    /**
     * Kiểm tra xem 1 URL cụ thể đã được cache chưa.
     */
    public static boolean isCached(Context context, String url) {
        SimpleCache cache = getCache(context);
        boolean cached = cache.isCached(url, 0, Long.MAX_VALUE);
        Log.d(TAG, (cached ? "✅ CACHE HIT" : "❌ CACHE MISS") + " → " + url);
        return cached;
    }

    /**
     * Giải phóng cache khi app bị kill hoàn toàn.
     */
    public static synchronized void release() {
        if (simpleCache != null) {
            simpleCache.release();
            simpleCache = null;
            Log.d(TAG, "🗑️ Cache đã release");
        }
    }
}
