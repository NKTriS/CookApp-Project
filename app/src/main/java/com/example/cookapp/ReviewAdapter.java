package com.example.cookapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private List<Review> reviews;
    private OnReviewAction listener;

    public interface OnReviewAction {
        void onDeleteClick(Review review);
    }

    public ReviewAdapter(List<Review> reviews, OnReviewAction listener) {
        this.reviews = reviews;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStars, tvDate, tvContent;
        LinearLayout llActions, btnEdit, btnDelete;

        public ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tv_name);
            tvStars = view.findViewById(R.id.tv_stars);
            tvDate = view.findViewById(R.id.tv_date);
            tvContent = view.findViewById(R.id.tv_content);
            llActions = view.findViewById(R.id.ll_actions);
            btnEdit = view.findViewById(R.id.btn_edit);
            btnDelete = view.findViewById(R.id.btn_delete);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.tvName.setText(review.getAuthorName());
        
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < review.getRating(); i++) {
            stars.append("⭐");
        }
        holder.tvStars.setText(stars.toString());
        
        // Format time as relative
        String timeText = formatRelativeTime(review.getCreatedAt());
        if (timeText == null || timeText.isEmpty()) {
            timeText = review.getDate() != null ? review.getDate() : "";
        }
        holder.tvDate.setText(timeText);
        holder.tvContent.setText(review.getContent());

        if (review.isCurrentUser()) {
            holder.llActions.setVisibility(View.VISIBLE);
            if (holder.btnDelete != null) {
                holder.btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(review);
                });
            }
        } else {
            holder.llActions.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    /**
     * Chuyển timestamp thành dạng "X phút/giờ/ngày trước" hoặc "dd/MM/yyyy"
     * Hỗ trợ: ISO string, JS Date.toString(), Unix ms, Unix sec
     */
    public static String formatRelativeTime(String timeValue) {
        if (timeValue == null || timeValue.isEmpty()) return null;
        try {
            long timeMs = -1;
            String clean = timeValue.trim().replace("\"", "");

            // 1. Thử parse as number (Unix timestamp)
            try {
                long numVal = Long.parseLong(clean);
                if (numVal > 1_000_000_000_000L) {
                    timeMs = numVal; // milliseconds
                } else if (numVal > 1_000_000_000L) {
                    timeMs = numVal * 1000L; // seconds
                }
            } catch (NumberFormatException ignored) {}

            // 2. Thử parse ISO format: "2026-04-02T08:28:55.000Z"
            if (timeMs < 0 && clean.contains("T") && clean.length() >= 19) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String isoClean = clean;
                    if (isoClean.contains(".")) isoClean = isoClean.substring(0, isoClean.indexOf('.'));
                    if (isoClean.endsWith("Z")) isoClean = isoClean.substring(0, isoClean.length() - 1);
                    Date d = sdf.parse(isoClean);
                    if (d != null) timeMs = d.getTime();
                } catch (Exception ignored) {}
            }

            // 3. Thử parse JS Date.toString(): "Wed Mar 12 2025 07:00:00 GMT+0700"
            if (timeMs < 0) {
                // Cắt phần "(Giờ ...)" nếu có
                String jsClean = clean;
                int parenIdx = jsClean.indexOf('(');
                if (parenIdx > 0) jsClean = jsClean.substring(0, parenIdx).trim();

                String[] jsPatterns = {
                    "EEE MMM dd yyyy HH:mm:ss 'GMT'Z",
                    "EEE MMM dd yyyy HH:mm:ss z",
                    "EEE MMM dd yyyy HH:mm:ss"
                };
                for (String pattern : jsPatterns) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                        Date d = sdf.parse(jsClean);
                        if (d != null) { timeMs = d.getTime(); break; }
                    } catch (Exception ignored) {}
                }
            }

            if (timeMs < 0) return null;

            // Tính relative time
            long diffMs = System.currentTimeMillis() - timeMs;
            if (diffMs < 0) return "Vừa xong";
            long diffSec = diffMs / 1000;
            long diffMin = diffSec / 60;
            long diffHour = diffMin / 60;
            long diffDay = diffHour / 24;

            if (diffSec < 60) return "Vừa xong";
            if (diffMin < 60) return diffMin + " phút trước";
            if (diffHour < 24) return diffHour + " giờ trước";
            if (diffDay <= 7) return diffDay + " ngày trước";

            // > 7 ngày -> hiển thị ngày/tháng/năm
            SimpleDateFormat outFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return outFmt.format(new Date(timeMs));
        } catch (Exception e) {
            return null;
        }
    }
}
