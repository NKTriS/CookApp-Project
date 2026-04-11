package com.example.cookapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.CheckBox;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cookapp.api.Resource;
import com.example.cookapp.repository.RecipeRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class RecipeListActivity extends AppCompatActivity {

    // Danh sách gốc từ API/cache (không bao giờ sửa trực tiếp)
    private final List<Recipe> fullList    = new ArrayList<>();
    // Danh sách đang hiển thị
    private final List<Recipe> displayList = new ArrayList<>();

    // ── Unified Filter States ─────────────────────────────────────────────
    private String currentSearchText = "";
    private String currentDifficulty = "Tất cả";
    private boolean isVeg, isKeto, isLowCarb, isEatClean, isGlutenFree, isDairyFree, isSeafoodFree, isPeanutFree;

    private RecipeAdapter adapter;
    private RecipeRepository recipeRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipe_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rv_recipes), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        recipeRepo = new RecipeRepository(this);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_recipe);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_shop) {
                    startActivity(new Intent(this, ShopActivity.class)); return true;
                } else if (id == R.id.nav_fav) {
                    startActivity(new Intent(this, FavoritesActivity.class)); return true;
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(this, SettingsActivity.class)); return true;
                } else if (id == R.id.nav_recipe) { return true; }
                return false;
            });
        }

        RecyclerView recyclerView = findViewById(R.id.rv_recipes);
        if (recyclerView != null) recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RecipeAdapter(displayList, recipe -> {
            Intent intent = new Intent(this, RecipeDetailActivity.class);
            intent.putExtra("recipe_id", recipe.getId());
            startActivity(intent);
        });
        if (recyclerView != null) recyclerView.setAdapter(adapter);

        // Nếu màn hình được mở với category_id từ MainActivity
        int categoryId = getIntent().getIntExtra("category_id", -1);
        String categoryName = getIntent().getStringExtra("category_name");
        if (categoryId != -1 && categoryName != null) {
            TextView tvTitle = findViewById(R.id.tv_title);
            if (tvTitle != null) tvTitle.setText(categoryName);
        }

        loadRecipes(categoryId);
        setupSearch();
        setupFilterButton();
    }

    // ─────────────────────────────────────────────────────────────────────
    // DATA LOADING — API first, Room fallback (via RecipeRepository)
    // ─────────────────────────────────────────────────────────────────────

    private void loadRecipes(int categoryId) {
        RecyclerView rv = findViewById(R.id.rv_recipes);
        View pbLoading = rv; // hiển thị shimmer/spinner nếu muốn

        RecipeRepository.RecipeListCallback callback = result -> {
            if (result.status == Resource.Status.SUCCESS && result.data != null) {
                List<Recipe> loaded = new ArrayList<>(result.data);
                runOnUiThread(() -> {
                    fullList.clear();
                    fullList.addAll(loaded);
                    applyFilters(); // Reset then apply current global state filters
                });
            } else if (result.status == Resource.Status.ERROR) {
                runOnUiThread(() ->
                    Toast.makeText(this, "Không tải được dữ liệu: " + result.message, Toast.LENGTH_SHORT).show());
            }
        };

        if (categoryId != -1) {
            recipeRepo.getRecipesByCategory(categoryId, callback);
        } else {
            recipeRepo.getAllRecipes(callback);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // SEARCH — in-memory filter trên fullList (hỗ trợ tên, thời gian)
    // ─────────────────────────────────────────────────────────────────────

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.et_search);
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                currentSearchText = s.toString().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                currentSearchText = etSearch.getText().toString().trim();
                applyFilters();
                return true;
            }
            return false;
        });
    }

    private void applyFilters() {
        List<Recipe> result = new ArrayList<>();
        
        // Parse search query as number for optional time filtering
        int searchMaxTime = -1;
        try { searchMaxTime = Integer.parseInt(currentSearchText); } 
        catch (NumberFormatException ignored) {}

        String lowerSearch = currentSearchText.toLowerCase(java.util.Locale.ROOT);

        for (Recipe r : fullList) {
            // 1. Search Query Check (title OR ingredient name OR cook time)
            boolean matchesSearch = false;
            if (currentSearchText.isEmpty()) {
                matchesSearch = true;
            } else {
                // Tìm theo tên món
                if (r.getTitle() != null && StringUtil.containsIgnoreCaseAndAccents(r.getTitle(), lowerSearch)) {
                    matchesSearch = true;
                }
                // Tìm theo nguyên liệu
                if (!matchesSearch && r.getIngredients() != null) {
                    for (Recipe.IngredientInfo ing : r.getIngredients()) {
                        if (ing.name != null && StringUtil.containsIgnoreCaseAndAccents(ing.name, lowerSearch)) {
                            matchesSearch = true;
                            break;
                        }
                    }
                }
                // Tìm theo thời gian nấu (nhập số)
                if (!matchesSearch && searchMaxTime != -1) {
                    try {
                        String t = r.getTime() != null ? r.getTime().replaceAll("[^0-9]", "") : "0";
                        if (!t.isEmpty() && Integer.parseInt(t) <= searchMaxTime) {
                            matchesSearch = true;
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (!matchesSearch) continue;

            // 2. Difficulty Check
            if (!"Tất cả".equals(currentDifficulty)) {
                if (r.getDifficulty() == null || !currentDifficulty.equalsIgnoreCase(r.getDifficulty())) {
                    continue;
                }
            }

            // 3. Strict Diet & Exclusion Flags (DB-backed tags)
            boolean skip = false;

            // 4A. Dietary Types (OR logic: if user checks any, recipe must match AT LEAST ONE selected diet)
            boolean dietSelected = isVeg || isKeto || isLowCarb || isEatClean;
            if (dietSelected) {
                boolean matchAnyDiet = false;
                
                if (isVeg && r.isVegetarian()) matchAnyDiet = true;
                if (isKeto && r.isKeto()) matchAnyDiet = true;
                if (isLowCarb && r.isLowCarb()) matchAnyDiet = true;
                if (isEatClean && r.isEatClean()) matchAnyDiet = true;
                
                if (!matchAnyDiet) skip = true;
            }

            if (skip) continue;

            // 4B. Exclusions (AND logic: if checked, recipe MUST HAVE the exclusion tag)
            // Note: DB tags like "Không Gluten" imply the recipe is safe (free of Gluten). 
            // So if user checks "Không Gluten", they expect recipes that HAVE the "Không Gluten" tag.
            // This acts exactly like a strict AND filter.
            if (isGlutenFree && !r.isGlutenFree()) skip = true;
            if (isDairyFree && !r.isDairyFree()) skip = true;
            if (isSeafoodFree && !r.isSeafoodFree()) skip = true;
            if (isPeanutFree && !r.isPeanutFree()) skip = true;

            if (skip) continue;

            // Matches ALL conditions
            result.add(r);
        }
        
        updateDisplay(result);
    }

    private boolean containsWords(Recipe r, String... words) {
        if (words == null || words.length == 0) return false;

        // Quét tiêu đề món ăn
        if (r.getTitle() != null) {
            String titleLower = r.getTitle().toLowerCase(java.util.Locale.ROOT);
            for (String w : words) {
                if (StringUtil.containsIgnoreCaseAndAccents(titleLower, w)) return true;
                // Double check without accents just in case
                if (titleLower.contains(w.toLowerCase())) return true; 
            }
        }
        
        // Quét thành phần nguyên liệu
        if (r.getIngredients() != null) {
            for (Recipe.IngredientInfo ing : r.getIngredients()) {
                if (ing.name != null) {
                    String nameLower = ing.name.toLowerCase(java.util.Locale.ROOT);
                    for (String w : words) {
                        if (StringUtil.containsIgnoreCaseAndAccents(nameLower, w)) return true;
                        if (nameLower.contains(w.toLowerCase())) return true;
                    }
                }
            }
        }
        return false;
    }

    private void setupFilterButton() {
        View btnFilter = findViewById(R.id.btn_filter);
        if (btnFilter == null) return;

        btnFilter.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_filter, null);
            AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
                .setView(dialogView).create();
            if (dialog.getWindow() != null)
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            View btnClose = dialogView.findViewById(R.id.btn_close);
            if (btnClose != null) btnClose.setOnClickListener(x -> dialog.dismiss());

            // Pre-fill previously selected values
            setCheckboxState(dialogView, R.id.cb_vegetarian, isVeg);
            setCheckboxState(dialogView, R.id.cb_keto, isKeto);
            setCheckboxState(dialogView, R.id.cb_lowcarb, isLowCarb);
            setCheckboxState(dialogView, R.id.cb_eatclean, isEatClean);
            setCheckboxState(dialogView, R.id.cb_gluten_free, isGlutenFree);
            setCheckboxState(dialogView, R.id.cb_dairy_free, isDairyFree);
            setCheckboxState(dialogView, R.id.cb_seafood_free, isSeafoodFree);
            setCheckboxState(dialogView, R.id.cb_peanut_free, isPeanutFree);

            View btnApply = dialogView.findViewById(R.id.btn_apply_filter);
            if (btnApply != null) {
                btnApply.setOnClickListener(x -> {
                    isVeg      = isChecked(dialogView, R.id.cb_vegetarian);
                    isKeto     = isChecked(dialogView, R.id.cb_keto);
                    isLowCarb  = isChecked(dialogView, R.id.cb_lowcarb);
                    isEatClean = isChecked(dialogView, R.id.cb_eatclean);
                    isGlutenFree = isChecked(dialogView, R.id.cb_gluten_free);
                    isDairyFree  = isChecked(dialogView, R.id.cb_dairy_free);
                    isSeafoodFree = isChecked(dialogView, R.id.cb_seafood_free);
                    isPeanutFree  = isChecked(dialogView, R.id.cb_peanut_free);

                    applyFilters();
                    Toast.makeText(this, "Tìm thấy " + displayList.size() + " công thức", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }

            dialog.show();
        });

        TextView spinnerDifficulty = findViewById(R.id.spinner_difficulty);
        if (spinnerDifficulty != null) {
            spinnerDifficulty.setText(currentDifficulty + " ▼");
            spinnerDifficulty.setOnClickListener(v -> {
                String[] difficulties = {"Tất cả", "Dễ", "Trung bình", "Khó"};
                new AlertDialog.Builder(this)
                    .setTitle("Chọn Độ khó")
                    .setItems(difficulties, (d, which) -> {
                        currentDifficulty = difficulties[which];
                        spinnerDifficulty.setText(currentDifficulty + " ▼");
                        applyFilters();
                    })
                    .show();
            });
        }
    }

    private void updateDisplay(List<Recipe> list) {
        displayList.clear();
        displayList.addAll(list);
        adapter.notifyDataSetChanged();
    }

    private void setCheckboxState(View parent, int id, boolean checked) {
        View v = parent.findViewById(id);
        if (v instanceof CheckBox) {
            ((CheckBox) v).setChecked(checked);
        }
    }

    private boolean isChecked(View parent, int id) {
        View v = parent.findViewById(id);
        return v instanceof CheckBox && ((CheckBox) v).isChecked();
    }
}
