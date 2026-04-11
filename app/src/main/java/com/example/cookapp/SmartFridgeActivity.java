package com.example.cookapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.SmartFridgeRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SmartFridgeActivity extends AppCompatActivity {

    private EditText etIngredients;
    private RecyclerView rvResults;
    private LinearLayout layoutEmptyState;
    private RecipeAdapter adapter;
    private List<Recipe> recipeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_smart_fridge);
        if (SessionManager.requireLogin(this)) { finish(); return; }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top + v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(v -> finish());

        etIngredients = findViewById(R.id.et_ingredients);
        rvResults = findViewById(R.id.rv_results);
        layoutEmptyState = findViewById(R.id.layout_empty_state);

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(recipeList, recipe -> {
            Intent intent = new Intent(this, RecipeDetailActivity.class);
            intent.putExtra("recipe_id", recipe.getId());
            startActivity(intent);
        });
        rvResults.setAdapter(adapter);

        findViewById(R.id.btn_analyze).setOnClickListener(v -> {
            String input = etIngredients.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập ít nhất 1 nguyên liệu!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etIngredients.getWindowToken(), 0);
            }

            analyzeIngredients(input);
        });
    }

    private void analyzeIngredients(String input) {
        // Tách chuỗi bằng dấu phẩy
        String[] parts = input.split(",");
        List<String> ingredients = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                ingredients.add(trimmed);
            }
        }

        if (ingredients.isEmpty()) return;

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getSmartFridgeRecommendations(new SmartFridgeRequest(ingredients)).enqueue(new Callback<List<Recipe>>() {
            @Override
            public void onResponse(Call<List<Recipe>> call, Response<List<Recipe>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Recipe> results = response.body();
                    recipeList.clear();
                    recipeList.addAll(results);
                    adapter.notifyDataSetChanged();

                    if (results.isEmpty()) {
                        layoutEmptyState.setVisibility(View.VISIBLE);
                        rvResults.setVisibility(View.GONE);
                        Toast.makeText(SmartFridgeActivity.this, "Không tìm thấy món nào phù hợp :(", Toast.LENGTH_SHORT).show();
                    } else {
                        layoutEmptyState.setVisibility(View.GONE);
                        rvResults.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(SmartFridgeActivity.this, "Lỗi phân tích từ máy chủ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Recipe>> call, Throwable t) {
                Toast.makeText(SmartFridgeActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
