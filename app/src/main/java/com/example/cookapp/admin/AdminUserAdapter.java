package com.example.cookapp.admin;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.R;
import com.example.cookapp.api.dto.UserDto;

import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.VH> {

    private List<UserDto> items;
    private final OnUserActionListener listener;

    public interface OnUserActionListener {
        void onToggleRole(UserDto user);
        void onDelete(UserDto user);
    }

    public AdminUserAdapter(List<UserDto> items, OnUserActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<UserDto> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        UserDto u = items.get(pos);
        h.tvName.setText(u.fullName != null && !u.fullName.isEmpty() ? u.fullName : "—");
        h.tvEmail.setText(u.email != null ? u.email : "—");
        h.tvPhone.setText(u.phoneNumber != null ? u.phoneNumber : "");

        // Role badge
        boolean isAdmin = "admin".equals(u.role);
        h.tvRole.setText(isAdmin ? "Admin" : "User");
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(40);
        if (isAdmin) {
            bg.setColor(0x1AFF7043);
            h.tvRole.setTextColor(0xFFFF7043);
        } else {
            bg.setColor(0x268B8FA3);
            h.tvRole.setTextColor(0xFF8B8FA3);
        }
        h.tvRole.setBackground(bg);

        // Toggle role button
        h.btnToggleRole.setText(isAdmin ? "→ User" : "→ Admin");
        h.btnToggleRole.setOnClickListener(v -> { if (listener != null) listener.onToggleRole(u); });
        h.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(u); });
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvPhone, tvRole, btnToggleRole, btnDelete;
        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_name);
            tvEmail = v.findViewById(R.id.tv_email);
            tvPhone = v.findViewById(R.id.tv_phone);
            tvRole = v.findViewById(R.id.tv_role);
            btnToggleRole = v.findViewById(R.id.btn_toggle_role);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
