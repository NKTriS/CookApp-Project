package com.example.cookapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.dto.AuthResponse;
import com.example.cookapp.repository.AuthRepository;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) ivBack.setOnClickListener(v -> finish());

        EditText etName            = findViewById(R.id.et_name);
        EditText etEmail           = findViewById(R.id.et_email);
        EditText etPassword        = findViewById(R.id.et_password);
        EditText etConfirmPassword = findViewById(R.id.et_confirm_password);

        Button btnRegister = findViewById(R.id.btn_register);
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> {
                String name     = etName            != null ? etName.getText().toString().trim()            : "";
                String email    = etEmail           != null ? etEmail.getText().toString().trim()           : "";
                String password = etPassword        != null ? etPassword.getText().toString().trim()        : "";
                String confirm  = etConfirmPassword != null ? etConfirmPassword.getText().toString().trim() : "";

                // Validation
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(this, "Vui lòng nhập họ tên!", Toast.LENGTH_SHORT).show(); return;
                }
                if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Email không hợp lệ!", Toast.LENGTH_SHORT).show(); return;
                }
                if (password.length() < 6) {
                    Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show(); return;
                }
                if (!password.equals(confirm)) {
                    Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show(); return;
                }

                final String finalName     = name;
                final String finalEmail    = email;
                final String finalPassword = password;

                AuthRepository authRepo = new AuthRepository(RegisterActivity.this);
                btnRegister.setEnabled(false);
                authRepo.register(finalEmail, finalPassword, finalName, null, null, result -> {
                    if (result.status == Resource.Status.LOADING) {
                        btnRegister.setText("Đang đăng ký...");
                    } else if (result.status == Resource.Status.SUCCESS) {
                        AuthResponse resp = result.data;
                        if (resp != null && resp.user != null) {
                            SessionManager session = new SessionManager(RegisterActivity.this);
                            session.saveUserProfile(resp.user.id, resp.user.fullName, resp.user.email, resp.token);
                            Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Chào " + resp.user.fullName, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    } else if (result.status == Resource.Status.ERROR) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Đăng ký");
                        Toast.makeText(RegisterActivity.this, result.message, Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        TextView tvLogin = findViewById(R.id.tv_login);
        if (tvLogin != null) tvLogin.setOnClickListener(v -> finish());
    }
}
