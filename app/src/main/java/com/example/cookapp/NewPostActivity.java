package com.example.cookapp;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.dto.PostDto;
import com.example.cookapp.repository.CommunityRepository;

public class NewPostActivity extends AppCompatActivity {
    private ImageView ivPreview;
    private String selectedImageUri = null;
    private CommunityRepository communityRepo;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    selectedImageUri = imageUri.toString();
                    ivPreview.setImageURI(imageUri);
                    ivPreview.setVisibility(View.VISIBLE);
                }
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_post);
        if (SessionManager.requireLogin(this)) { finish(); return; }

        communityRepo = new CommunityRepository(this);

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        ivPreview = findViewById(R.id.iv_preview);

        // Hiển thị tên người dùng từ cache
        TextView tvAuthorName = findViewById(R.id.tv_author_name);
        SessionManager session = new SessionManager(this);
        if (tvAuthorName != null) {
            String cached = session.getCachedUserName();
            if (cached != null && !cached.isEmpty()) {
                tvAuthorName.setText(cached);
            }
        }

        LinearLayout btnPickImage = findViewById(R.id.btn_pick_image);
        if (btnPickImage != null) {
            btnPickImage.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                imagePickerLauncher.launch(intent);
            });
        }

        EditText etTitle = findViewById(R.id.et_title);
        EditText etContent   = findViewById(R.id.et_content);
        LinearLayout btnPost = findViewById(R.id.btn_post);
        if (btnPost != null && etContent != null) {
            btnPost.setOnClickListener(v -> {
                if (!session.isLoggedIn()) {
                    Toast.makeText(this, "Vui lòng đăng nhập để đăng bài!", Toast.LENGTH_SHORT).show();
                    return;
                }
                String content = etContent.getText().toString().trim();
                String rawTitle = etTitle != null ? etTitle.getText().toString().trim() : "";

                if (content.isEmpty() && selectedImageUri == null && rawTitle.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập nội dung, tiêu đề hoặc chọn ảnh!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (rawTitle.isEmpty()) {
                    rawTitle = (content.length() > 20) ? content.substring(0, 20) + "..." : "Một hình ảnh mới";
                }

                PostDto newPost  = new PostDto();
                newPost.title    = rawTitle;
                newPost.content  = content;
                newPost.author   = session.getCachedUserName();

                String finalImageUrl = null;
                if (selectedImageUri != null) {
                    try {
                        Uri uri = Uri.parse(selectedImageUri);
                        InputStream is = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int width = bitmap.getWidth();
                        int height = bitmap.getHeight();
                        if (width > 800) {
                            height = (int) (height * (800.0 / width));
                            width = 800;
                            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                        }
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                        byte[] bytes = baos.toByteArray();
                        finalImageUrl = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
                        is.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                newPost.image_url = finalImageUrl;

                btnPost.setEnabled(false);
                communityRepo.createPost(newPost, result -> {
                    if (result.status == Resource.Status.SUCCESS) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Đã đăng bài viết!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    } else if (result.status == Resource.Status.ERROR) {
                        runOnUiThread(() -> {
                            btnPost.setEnabled(true);
                            Toast.makeText(this, "Lỗi đăng bài: " + result.message, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            });
        }
    }
}
