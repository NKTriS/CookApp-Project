package com.example.cookapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.dto.AdminRecipeCreateDto;
import com.example.cookapp.api.dto.GenericResponse;
import com.example.cookapp.api.dto.RecipeMetadataResponse;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import android.content.Intent;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình Admin thêm công thức mới.
 *
 * Activity này cho phép quản trị viên nhập thông tin công thức, chọn ảnh/video,
 * thêm nguyên liệu, thêm các bước nấu kèm timer_seconds và video_start_time,
 * sau đó gửi dữ liệu lên API admin bằng multipart/form-data.
 */
public class AdminAddRecipeActivity extends AppCompatActivity {

    private ApiService apiService;
    private ProgressBar progressOverlay;

    private EditText etTitle, etDesc, etTime, etServings;
    private EditText etCal, etProtein, etFat, etCarbs, etFiber, etSugar;
    private Spinner spinnerDifficulty;
    
    private ImageView ivRecipeImage;
    private TextView tvVideoPath;
    
    private Uri imageUri = null;
    private Uri videoUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    ivRecipeImage.setImageURI(uri);
                }
            }
    );

    private final ActivityResultLauncher<String> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    videoUri = uri;
                    tvVideoPath.setText("Đã nhận file video");
                }
            }
    );
    
    // Containers
    private LinearLayout containerCategories, containerTags, containerIngredients, containerSteps;
    
    // Metadata cached
    private List<RecipeMetadataResponse.MetadataItem> globalIngredients = new ArrayList<>();
    
    // Track dynamically added views
    private List<CheckBox> categoryCheckboxes = new ArrayList<>();
    private List<CheckBox> tagCheckboxes = new ArrayList<>();
    private List<View> ingredientRows = new ArrayList<>();
    private List<View> stepRows = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_recipe);

        apiService = RetrofitClient.getClient(this).create(ApiService.class);
        
        initViews();
        setupBasicSpinners();
        
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        
        findViewById(R.id.btn_add_ingredient).setOnClickListener(v -> addIngredientRow());
        findViewById(R.id.btn_add_step).setOnClickListener(v -> addStepRow());
        findViewById(R.id.btn_submit).setOnClickListener(v -> submitRecipe());

        loadMetadata();
    }

    private void initViews() {
        progressOverlay = findViewById(R.id.progress_overlay);
        
        etTitle = findViewById(R.id.et_title);
        etDesc = findViewById(R.id.et_desc);
        etTime = findViewById(R.id.et_time);
        etServings = findViewById(R.id.et_servings);
        
        ivRecipeImage = findViewById(R.id.iv_recipe_image);
        tvVideoPath = findViewById(R.id.tv_video_path);
        
        ivRecipeImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        findViewById(R.id.btn_pick_video).setOnClickListener(v -> pickVideoLauncher.launch("video/*"));
        
        spinnerDifficulty = findViewById(R.id.spinner_difficulty);
        
        etCal = findViewById(R.id.et_cal);
        etProtein = findViewById(R.id.et_protein);
        etFat = findViewById(R.id.et_fat);
        etCarbs = findViewById(R.id.et_carbs);
        etFiber = findViewById(R.id.et_fiber);
        etSugar = findViewById(R.id.et_sugar);
        
        containerCategories = findViewById(R.id.container_categories);
        containerTags = findViewById(R.id.container_tags);
        containerIngredients = findViewById(R.id.container_ingredients);
        containerSteps = findViewById(R.id.container_steps);
    }

    private void setupBasicSpinners() {
        String[] difficulties = {"Rất dễ", "Dễ", "Trung bình", "Khó"};
        ArrayAdapter<String> diffAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, difficulties);
        diffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(diffAdapter);
    }

    /**
     * Gọi API GET /api/admin/recipe-metadata để lấy danh mục, chế độ ăn và nguyên liệu.
     * Dữ liệu này dùng để dựng checkbox và gợi ý AutoComplete khi admin nhập công thức.
     */
    private void loadMetadata() {
        showProgress(true);
        apiService.getRecipeMetadata().enqueue(new Callback<RecipeMetadataResponse>() {
            @Override
            public void onResponse(Call<RecipeMetadataResponse> call, Response<RecipeMetadataResponse> response) {
                showProgress(false);
                if (response.isSuccessful() && response.body() != null) {
                    populateCheckboxes(response.body().categories, containerCategories, categoryCheckboxes);
                    populateCheckboxes(response.body().dietTypes, containerTags, tagCheckboxes);
                    
                    if (response.body().ingredients != null) {
                        globalIngredients.clear();
                        globalIngredients.addAll(response.body().ingredients);
                        if(ingredientRows.isEmpty()) addIngredientRow(); // Add first basic row
                    }
                    if(stepRows.isEmpty()) addStepRow(); // Add first basic step
                } else {
                    Toast.makeText(AdminAddRecipeActivity.this, "Lỗi tải metadata", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RecipeMetadataResponse> call, Throwable t) {
                showProgress(false);
                Toast.makeText(AdminAddRecipeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Tạo danh sách checkbox động cho category hoặc diet type.
     * Mỗi checkbox lưu id ở tag để khi submit có thể gửi đúng khóa ngoại về backend.
     */
    private void populateCheckboxes(List<RecipeMetadataResponse.MetadataItem> items, LinearLayout container, List<CheckBox> trackers) {
        if (items == null) return;
        for (RecipeMetadataResponse.MetadataItem item : items) {
            CheckBox cb = new CheckBox(this);
            cb.setText(item.name);
            cb.setTextColor(android.graphics.Color.WHITE);
            cb.setTag(item.id);
            container.addView(cb);
            trackers.add(cb);
        }
    }

    /**
     * Thêm một dòng nhập nguyên liệu.
     * Dòng này gồm tên nguyên liệu có AutoComplete, số lượng, đơn vị và nút xóa.
     */
    private void addIngredientRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 8, 0, 8);

        // AutoComplete Name
        AutoCompleteTextView autoComplete = new AutoCompleteTextView(this);
        LinearLayout.LayoutParams spParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f);
        autoComplete.setLayoutParams(spParams);
        ArrayAdapter<RecipeMetadataResponse.MetadataItem> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, globalIngredients);
        autoComplete.setAdapter(adapter);
        autoComplete.setHint("Tên nguyên liệu");
        autoComplete.setHintTextColor(android.graphics.Color.GRAY);
        autoComplete.setTextColor(android.graphics.Color.WHITE);
        autoComplete.setThreshold(1);
        autoComplete.setDropDownBackgroundResource(android.R.color.background_dark);

        // EditText Quantity
        EditText etQuantity = new EditText(this);
        etQuantity.setHint("SL");
        etQuantity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etQuantity.setTextColor(android.graphics.Color.WHITE);
        etQuantity.setHintTextColor(android.graphics.Color.GRAY);
        LinearLayout.LayoutParams qParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        etQuantity.setLayoutParams(qParams);

        // EditText Unit
        EditText etUnit = new EditText(this);
        etUnit.setHint("Đơn vị");
        etUnit.setTextColor(android.graphics.Color.WHITE);
        etUnit.setHintTextColor(android.graphics.Color.GRAY);
        LinearLayout.LayoutParams uParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        etUnit.setLayoutParams(uParams);

        // Delete button
        ImageView ivDelete = new ImageView(this);
        ivDelete.setImageResource(android.R.drawable.ic_menu_delete);
        ivDelete.setColorFilter(android.graphics.Color.RED);
        ivDelete.setPadding(16, 16, 16, 16);
        ivDelete.setOnClickListener(v -> {
            containerIngredients.removeView(row);
            ingredientRows.remove(row);
        });

        row.addView(autoComplete);
        row.addView(etQuantity);
        row.addView(etUnit);
        row.addView(ivDelete);

        containerIngredients.addView(row);
        ingredientRows.add(row);
    }

    /**
     * Thêm một dòng nhập bước nấu.
     * Mỗi bước gồm tiêu đề ngắn, hướng dẫn chi tiết, thời lượng hẹn giờ theo phút
     * và mốc giây video để Cooking Mode tua đúng đoạn minh họa.
     */
    private void addStepRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 8, 0, 16);

        LinearLayout rowTop = new LinearLayout(this);
        rowTop.setOrientation(LinearLayout.HORIZONTAL);
        rowTop.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rowTop.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvIndex = new TextView(this);
        tvIndex.setText("B" + (stepRows.size() + 1) + ": ");
        tvIndex.setTextColor(android.graphics.Color.WHITE);
        tvIndex.setTextSize(16);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Tóm tắt (Ví dụ: Sơ chế gà)");
        etTitle.setTextColor(android.graphics.Color.WHITE);
        etTitle.setHintTextColor(android.graphics.Color.GRAY);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        etTitle.setLayoutParams(titleParams);
        
        EditText etInstruction = new EditText(this);
        etInstruction.setHint("Chi tiết (Khoảng 700g gà...)");
        etInstruction.setTextColor(android.graphics.Color.WHITE);
        etInstruction.setHintTextColor(android.graphics.Color.GRAY);
        LinearLayout.LayoutParams txtParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        txtParams.leftMargin = (int) (tvIndex.getPaint().measureText("B10: ") > 0 ? tvIndex.getPaint().measureText("B10: ") : 80);
        etInstruction.setLayoutParams(txtParams);

        ImageView ivDelete = new ImageView(this);
        ivDelete.setImageResource(android.R.drawable.ic_menu_delete);
        ivDelete.setColorFilter(android.graphics.Color.RED);
        ivDelete.setPadding(16, 16, 16, 16);
        ivDelete.setOnClickListener(v -> {
            containerSteps.removeView(row);
            stepRows.remove(row);
        });

        rowTop.addView(tvIndex);
        rowTop.addView(etTitle);
        rowTop.addView(ivDelete);

        LinearLayout rowMid = new LinearLayout(this);
        rowMid.setOrientation(LinearLayout.HORIZONTAL);
        rowMid.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rowMid.addView(etInstruction);

        LinearLayout rowBottom = new LinearLayout(this);
        rowBottom.setOrientation(LinearLayout.HORIZONTAL);
        rowBottom.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // Add a spacer to align with instruction
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(tvIndex.getPaint().measureText("B10: ") > 0 ? (int)tvIndex.getPaint().measureText("B10: ") : 100, 10));
        
        EditText etTimer = new EditText(this);
        etTimer.setHint("Hẹn Giờ (Phút)");
        etTimer.setTextColor(android.graphics.Color.WHITE);
        etTimer.setHintTextColor(android.graphics.Color.GRAY);
        etTimer.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etTimer.setBackgroundResource(R.drawable.bg_rounded_8dp_d9d9d9);
        etTimer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF242838));
        etTimer.setGravity(android.view.Gravity.CENTER);
        etTimer.setTextSize(12);
        LinearLayout.LayoutParams timerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        timerParams.leftMargin = 4;
        timerParams.rightMargin = 8;
        etTimer.setLayoutParams(timerParams);

        EditText etVideoStart = new EditText(this);
        etVideoStart.setHint("Giây Video");
        etVideoStart.setTextColor(android.graphics.Color.WHITE);
        etVideoStart.setHintTextColor(android.graphics.Color.GRAY);
        etVideoStart.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etVideoStart.setBackgroundResource(R.drawable.bg_rounded_8dp_d9d9d9);
        etVideoStart.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF242838));
        etVideoStart.setGravity(android.view.Gravity.CENTER);
        etVideoStart.setTextSize(12);
        LinearLayout.LayoutParams videoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        videoParams.leftMargin = 8;
        videoParams.rightMargin = 4;
        etVideoStart.setLayoutParams(videoParams);

        rowBottom.addView(spacer);
        rowBottom.addView(etTimer);
        rowBottom.addView(etVideoStart);

        row.addView(rowTop);
        row.addView(rowMid);
        row.addView(rowBottom);

        containerSteps.addView(row);
        stepRows.add(row);
    }

    private int parseIntSafe(EditText et) {
        String val = et.getText().toString().trim();
        if (val.isEmpty()) return 0;
        try { return Integer.parseInt(val); } catch (Exception e) { return 0; }
    }

    private float parseFloatSafe(EditText et) {
        String val = et.getText().toString().trim();
        if (val.isEmpty()) return 0f;
        try { return Float.parseFloat(val); } catch (Exception e) { return 0f; }
    }

    /**
     * Thu thập toàn bộ dữ liệu trên form và gửi lên API POST /api/admin/recipes.
     *
     * Phần JSON chứa thông tin công thức, dinh dưỡng, danh mục, nguyên liệu và bước nấu.
     * Ảnh/video được chuyển thành MultipartBody.Part để backend lưu file và tạo URL.
     */
    private void submitRecipe() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Snackbar.make(etTitle, "Vui lòng nhập tên công thức", Snackbar.LENGTH_SHORT).show();
            return;
        }

        AdminRecipeCreateDto dto = new AdminRecipeCreateDto();
        dto.title = title;
        dto.description = etDesc.getText().toString().trim();
        dto.cookTime = parseIntSafe(etTime);
        dto.difficulty = spinnerDifficulty.getSelectedItem().toString();
        dto.servings = parseIntSafe(etServings);
        dto.calories = parseIntSafe(etCal);

        // Nhóm thông tin dinh dưỡng sẽ được backend lưu vào bảng nutrition_facts.
        dto.nutritionFacts = new AdminRecipeCreateDto.NutritionFactsDto();
        dto.nutritionFacts.calories = dto.calories;
        dto.nutritionFacts.protein = parseIntSafe(etProtein);
        dto.nutritionFacts.fat = parseIntSafe(etFat);
        dto.nutritionFacts.carbs = parseIntSafe(etCarbs);
        dto.nutritionFacts.fiber = parseFloatSafe(etFiber);
        dto.nutritionFacts.sugar = parseFloatSafe(etSugar);
        
        // Danh mục và chế độ ăn được gửi dưới dạng danh sách id để tạo bản ghi liên kết.
        dto.categoryIds = new ArrayList<>();
        for (CheckBox cb : categoryCheckboxes) {
            if (cb.isChecked()) dto.categoryIds.add((Integer) cb.getTag());
        }

        dto.dietTypeIds = new ArrayList<>();
        for (CheckBox cb : tagCheckboxes) {
            if (cb.isChecked()) dto.dietTypeIds.add((Integer) cb.getTag());
        }

        // Mỗi bước nấu được gửi kèm timerSeconds và videoStartTime để phục vụ Cooking Mode.
        dto.steps = new ArrayList<>();
        for (View row : stepRows) {
            if (row instanceof LinearLayout) {
                LinearLayout parentLinear = (LinearLayout) row;
                LinearLayout rowTop = (LinearLayout) parentLinear.getChildAt(0);
                LinearLayout rowMid = (LinearLayout) parentLinear.getChildAt(1);
                LinearLayout rowBottom = (LinearLayout) parentLinear.getChildAt(2);
                
                EditText etStepTitle = (EditText) rowTop.getChildAt(1);
                EditText etInst = (EditText) rowMid.getChildAt(0);
                EditText etTimer = (EditText) rowBottom.getChildAt(1);
                EditText etVideoStart = (EditText) rowBottom.getChildAt(2);
                
                String stepTitle = etStepTitle.getText().toString().trim();
                String ins = etInst.getText().toString().trim();
                if (!ins.isEmpty() || !stepTitle.isEmpty()) {
                    AdminRecipeCreateDto.StepDto s = new AdminRecipeCreateDto.StepDto();
                    s.title = stepTitle;
                    s.instruction = ins;
                    s.timerSeconds = parseIntSafe(etTimer) * 60;
                    s.videoStartTime = parseIntSafe(etVideoStart);
                    dto.steps.add(s);
                }
            }
        }

        // Nguyên liệu có thể lấy theo id có sẵn hoặc theo tên mới để backend tự tạo.
        dto.ingredients = new ArrayList<>();
        for (View row : ingredientRows) {
            if (row instanceof LinearLayout) {
                LinearLayout l = (LinearLayout) row;
                AutoCompleteTextView ac = (AutoCompleteTextView) l.getChildAt(0);
                EditText etQ = (EditText) l.getChildAt(1);
                EditText etU = (EditText) l.getChildAt(2);

                String typedName = ac.getText().toString().trim();
                if (!typedName.isEmpty()) {
                    AdminRecipeCreateDto.IngredientDto idto = new AdminRecipeCreateDto.IngredientDto();
                    idto.ingredientName = typedName; // Send name safely to backend
                    
                    // Match with known ID if user selected from dropdown
                    for (RecipeMetadataResponse.MetadataItem item : globalIngredients) {
                        if (item.name.equalsIgnoreCase(typedName)) {
                            idto.ingredientId = item.id;
                            break;
                        }
                    }
                    
                    idto.quantity = parseIntSafe(etQ);
                    idto.unit = etU.getText().toString().trim();
                    dto.ingredients.add(idto);
                }
            }
        }

        // Basic validation
        if (dto.steps.isEmpty() || dto.ingredients.isEmpty()) {
            Snackbar.make(etTitle, "Cần tối thiểu 1 nguyên liệu và 1 bước thực hiện!", Snackbar.LENGTH_LONG).show();
            return;
        }

        showProgress(true);
        new Thread(() -> {
            try {
                String jsonBody = new Gson().toJson(dto);
                RequestBody dataBody = RequestBody.create(MediaType.parse("application/json"), jsonBody);
                
                // Chuyển ảnh thành multipart part tên "image" cho multer ở backend xử lý.
                MultipartBody.Part imagePart = null;
                if (imageUri != null) {
                    File file = getFileFromUri(imageUri, "img");
                    if (file != null) {
                        RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
                        imagePart = MultipartBody.Part.createFormData("image", file.getName(), reqFile);
                    }
                }

                // Chuyển video thành multipart part tên "video" để backend lưu vào public/videos.
                MultipartBody.Part videoPart = null;
                if (videoUri != null) {
                    File file = getFileFromUri(videoUri, "vid");
                    if (file != null) {
                        RequestBody reqFile = RequestBody.create(MediaType.parse("video/*"), file);
                        videoPart = MultipartBody.Part.createFormData("video", file.getName(), reqFile);
                    }
                }

                MultipartBody.Part finalImagePart = imagePart;
                MultipartBody.Part finalVideoPart = videoPart;
                
                runOnUiThread(() -> {
                    // Gọi API admin tạo công thức mới; RetrofitClient đã gắn token admin vào header.
                    apiService.createRecipe(dataBody, finalImagePart, finalVideoPart).enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                            showProgress(false);
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminAddRecipeActivity.this, "Tạo công thức thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AdminAddRecipeActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<GenericResponse> call, Throwable t) {
                            showProgress(false);
                            Toast.makeText(AdminAddRecipeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(AdminAddRecipeActivity.this, "Lỗi đọc file!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Sao chép file người dùng chọn từ Uri vào file tạm trong cache của app.
     * Retrofit cần File thật để tạo multipart upload ổn định trên nhiều phiên bản Android.
     */
    private File getFileFromUri(Uri uri, String prefix) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File tempFile = File.createTempFile(prefix, ".tmp", getCacheDir());
            tempFile.deleteOnExit();
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showProgress(boolean show) {
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_submit).setEnabled(!show);
    }
}
