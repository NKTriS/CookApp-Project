package com.example.cookapp.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.R;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.AdminStepsUpdateRequest;
import com.example.cookapp.api.dto.GenericResponse;
import com.example.cookapp.api.dto.RecipeStepDto;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminEditTimeActivity extends AppCompatActivity {

    private int recipeId;
    private String recipeTitle;

    private ImageView btnBack;
    private TextView tvRecipeTitle;
    private ProgressBar progressBar;
    private RecyclerView rvSteps;
    private AppCompatButton btnSave;

    private AdminEditStepAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_time);

        recipeId = getIntent().getIntExtra("recipe_id", -1);
        recipeTitle = getIntent().getStringExtra("recipe_title");

        btnBack = findViewById(R.id.btn_back);
        tvRecipeTitle = findViewById(R.id.tv_recipe_title);
        progressBar = findViewById(R.id.progress_bar);
        rvSteps = findViewById(R.id.rv_steps);
        btnSave = findViewById(R.id.btn_save);

        if (recipeTitle != null) {
            tvRecipeTitle.setText(recipeTitle);
        }

        btnBack.setOnClickListener(v -> finish());
        
        adapter = new AdminEditStepAdapter();
        rvSteps.setLayoutManager(new LinearLayoutManager(this));
        rvSteps.setAdapter(adapter);

        btnSave.setOnClickListener(v -> saveTimes());

        if (recipeId != -1) {
            loadSteps();
        } else {
            Toast.makeText(this, "Lỗi ID công thức", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadSteps() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getRecipeSteps(recipeId).enqueue(new Callback<List<RecipeStepDto>>() {
            @Override
            public void onResponse(Call<List<RecipeStepDto>> call, Response<List<RecipeStepDto>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setItems(response.body());
                } else {
                    Toast.makeText(AdminEditTimeActivity.this, "Không thể tải các bước", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<RecipeStepDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminEditTimeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTimes() {
        if (adapter.getItems() == null || adapter.getItems().isEmpty()) {
            Toast.makeText(this, "Không có bước nào để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable save button to prevent double clicks
        btnSave.setEnabled(false);
        btnSave.setText("ĐANG LƯU...");

        AdminStepsUpdateRequest request = new AdminStepsUpdateRequest(adapter.getItems());

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.updateRecipeSteps(recipeId, request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                btnSave.setEnabled(true);
                btnSave.setText("LƯU THỜI GIAN");
                if (response.isSuccessful()) {
                    Toast.makeText(AdminEditTimeActivity.this, "✅ Lưu thời gian thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AdminEditTimeActivity.this, "Lỗi máy chủ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText("LƯU THỜI GIAN");
                Toast.makeText(AdminEditTimeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
