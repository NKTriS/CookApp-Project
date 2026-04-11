package com.example.cookapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.api.Resource;
import com.example.cookapp.api.dto.NotificationDto;
import com.example.cookapp.repository.NotificationRepository;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private NotificationAdapter adapter;
    private final List<NotificationModel> notifications = new ArrayList<>();
    private NotificationRepository notifRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification);
        if (SessionManager.requireLogin(this)) { finish(); return; }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bg_header), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem thông báo.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        notifRepo = new NotificationRepository(this);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_notifications);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            adapter = new NotificationAdapter(notifications, notif -> {
                notif.setRead(true);
                // Phase 3: Mark as read via API
                try {
                    int nId = Integer.parseInt(notif.getId());
                    notifRepo.markAsRead(nId, result -> {});
                } catch (NumberFormatException ignored) {}
                if (rv.getAdapter() != null) rv.getAdapter().notifyDataSetChanged();
            });
            rv.setAdapter(adapter);
        }

        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        notifRepo.getNotifications(result -> {
            if (result.status == Resource.Status.LOADING) {
                // Can show loading if desired
            } else if (result.status == Resource.Status.SUCCESS && result.data != null) {
                List<NotificationModel> loaded = new ArrayList<>();
                for (NotificationDto dto : result.data) {
                    loaded.add(new NotificationModel(
                        String.valueOf(dto.id),
                        dto.created_at != null ? dto.created_at : "Vừa xong",
                        dto.message,
                        dto.isRead,
                        dto.type != null ? dto.type : "system"
                    ));
                }
                runOnUiThread(() -> {
                    notifications.clear();
                    notifications.addAll(loaded);
                    if (adapter != null) adapter.notifyDataSetChanged();

                    // Show empty state
                    View emptyView = findViewById(R.id.tv_empty_notifications);
                    if (emptyView != null) {
                        emptyView.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
            } else if (result.status == Resource.Status.ERROR) {
                runOnUiThread(() ->
                    Toast.makeText(this, "Lỗi tải thông báo: " + result.message, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
}
