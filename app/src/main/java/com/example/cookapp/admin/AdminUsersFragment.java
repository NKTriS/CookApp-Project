package com.example.cookapp.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.R;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.AdminUsersResponse;
import com.example.cookapp.api.dto.GenericResponse;
import com.example.cookapp.api.dto.UserDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment quản lý người dùng trong Admin Panel.
 *
 * Fragment này hỗ trợ tìm kiếm người dùng, đổi vai trò user/admin và xóa tài khoản.
 * Các thao tác đều gọi API admin và yêu cầu token có quyền quản trị.
 */
public class AdminUsersFragment extends Fragment implements AdminUserAdapter.OnUserActionListener {

    private RecyclerView rvUsers;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private EditText etSearch;
    private AdminUserAdapter adapter;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvUsers = view.findViewById(R.id.rv_users);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        etSearch = view.findViewById(R.id.et_search);

        adapter = new AdminUserAdapter(new ArrayList<>(), this);
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUsers.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                // Debounce 400ms để tránh gọi API liên tục trong lúc admin đang gõ tìm kiếm.
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> loadUsers(s.toString().trim());
                searchHandler.postDelayed(searchRunnable, 400);
            }
        });

        loadUsers("");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadUsers(etSearch.getText().toString().trim());
        }
    }

    /**
     * Gọi API GET /api/admin/users để tải danh sách người dùng theo từ khóa tìm kiếm.
     */
    private void loadUsers(String search) {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        api.getAdminUsers(1, 50, search).enqueue(new Callback<AdminUsersResponse>() {
            @Override
            public void onResponse(Call<AdminUsersResponse> call, Response<AdminUsersResponse> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setItems(response.body().users);
                    tvEmpty.setVisibility(response.body().users.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<AdminUsersResponse> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Đổi vai trò người dùng giữa user và admin bằng API PATCH /api/admin/users/{id}/role.
     */
    @Override
    public void onToggleRole(UserDto user) {
        String newRole = "admin".equals(user.role) ? "user" : "admin";
        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi vai trò")
                .setMessage("Chuyển \"" + (user.fullName != null ? user.fullName : user.email)
                        + "\" thành " + newRole + "?")
                .setPositiveButton("Xác nhận", (d, w) -> {
                    ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
                    Map<String, String> body = new HashMap<>();
                    body.put("role", newRole);
                    api.updateUserRole(user.id, body).enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "Đã cập nhật ✓", Toast.LENGTH_SHORT).show();
                                loadUsers(etSearch.getText().toString().trim());
                            }
                        }
                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Lỗi", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Xóa tài khoản người dùng bằng API DELETE /api/admin/users/{id}.
     * Trước khi xóa có hộp thoại xác nhận vì thao tác này không thể hoàn tác.
     */
    @Override
    public void onDelete(UserDto user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa người dùng")
                .setMessage("Xóa \"" + (user.fullName != null ? user.fullName : user.email)
                        + "\"? Hành động không thể hoàn tác!")
                .setPositiveButton("Xóa", (d, w) -> {
                    ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
                    api.deleteUser(user.id).enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "Đã xóa ✓", Toast.LENGTH_SHORT).show();
                                loadUsers(etSearch.getText().toString().trim());
                            }
                        }
                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Lỗi", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
