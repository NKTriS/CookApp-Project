package com.example.cookapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onClick(NotificationModel model);
    }

    private List<NotificationModel> notifications;
    private OnNotificationClickListener listener;

    public NotificationAdapter(List<NotificationModel> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;
        FrameLayout flIconBg;
        TextView tvIconEmoji, tvMessage, tvTime;
        View vUnreadDot;

        public ViewHolder(View view) {
            super(view);
            container = view.findViewById(R.id.ll_container);
            flIconBg = view.findViewById(R.id.fl_icon_bg);
            tvIconEmoji = view.findViewById(R.id.tv_icon_emoji);
            tvMessage = view.findViewById(R.id.tv_message);
            tvTime = view.findViewById(R.id.tv_time);
            vUnreadDot = view.findViewById(R.id.v_unread_dot);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel notif = notifications.get(position);

        holder.tvMessage.setText(notif.getMessage());
        holder.tvMessage.setTypeface(null, notif.isRead() ? Typeface.NORMAL : Typeface.BOLD);
        holder.tvTime.setText(notif.getTimeAgo());

        holder.container.setBackgroundColor(notif.isRead() ? Color.TRANSPARENT : Color.parseColor("#E8F5FD"));
        holder.vUnreadDot.setVisibility(notif.isRead() ? View.GONE : View.VISIBLE);

        if ("like".equals(notif.getType())) {
            holder.tvIconEmoji.setText("❤️");
            holder.flIconBg.setBackgroundColor(Color.parseColor("#FFE4E4"));
        } else if ("comment".equals(notif.getType())) {
            holder.tvIconEmoji.setText("💬");
            holder.flIconBg.setBackgroundColor(Color.parseColor("#E4F0FF"));
        } else {
            holder.tvIconEmoji.setText("🔔");
            holder.flIconBg.setBackgroundColor(Color.parseColor("#E4F7E4"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(notif);
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }
}
