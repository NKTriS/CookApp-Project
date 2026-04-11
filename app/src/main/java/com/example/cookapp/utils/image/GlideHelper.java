package com.example.cookapp.utils.image;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.cookapp.R;

/**
 * Trung tâm load ảnh duy nhất của app.
 *
 * Nhận imageUrl có thể là:
 *   1. "local:ing_thit_ga"   → Tìm R.drawable.ing_thit_ga (ảnh local, 100% đúng)
 *   2. "https://..."         → Load URL qua mạng (fallback khi chưa có local)
 *   3. null / empty          → Hiện placeholder
 */
public class GlideHelper {

    /**
     * Load ảnh nguyên liệu (ingredient).
     */
    public static void loadIngredient(Context context, String imageUrl, ImageView imageView) {
        load(context, imageUrl, imageView, R.drawable.placeholder_ingredient);
    }

    /**
     * Load ảnh công thức (recipe).
     */
    public static void loadRecipe(Context context, String imageUrl, ImageView imageView) {
        load(context, imageUrl, imageView, R.drawable.placeholder_recipe);
    }

    /**
     * Load ảnh danh mục (category).
     */
    public static void loadCategory(Context context, String imageUrl, ImageView imageView) {
        load(context, imageUrl, imageView, R.drawable.placeholder_recipe);
    }

    /**
     * Generic loader — tự phát hiện local vs URL.
     */
    public static void load(Context context, String imageUrl, ImageView imageView, int placeholderRes) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(placeholderRes);
            return;
        }

        if (imageUrl.startsWith("local:")) {
            // Tìm drawable theo tên resource (e.g. "local:ing_thit_ga" → R.drawable.ing_thit_ga)
            String resName = imageUrl.substring(6); // strip "local:"
            int resId = context.getResources().getIdentifier(resName, "drawable", context.getPackageName());
            if (resId != 0) {
                Glide.with(context)
                        .load(resId)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(imageView);
            } else {
                // Drawable không tồn tại → fallback placeholder
                imageView.setImageResource(placeholderRes);
            }
            return;
        }

        // Rewrite URL to match current environment (MÁY THẬT / MÁY ẢO)
        String finalUrl = imageUrl;
        if (finalUrl != null) {
            String base = com.example.cookapp.utils.NetworkConfig.getBaseUrl();
            if (finalUrl.startsWith("/")) {
                finalUrl = base + finalUrl;
            } else if (finalUrl.startsWith("http://10.0.2.2:3000")) {
                finalUrl = finalUrl.replace("http://10.0.2.2:3000", base);
            } else if (finalUrl.startsWith("http://192.168.42.121:3000")) {
                finalUrl = finalUrl.replace("http://192.168.42.121:3000", base);
            }
        }

        // URL thông thường
        Glide.with(context)
                .load(finalUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .centerCrop()
                .into(imageView);
    }
}
