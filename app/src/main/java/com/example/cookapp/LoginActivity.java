package com.example.cookapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.ProgressBar;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.cookapp.api.Resource;
import com.example.cookapp.api.dto.AuthResponse;
import com.example.cookapp.repository.AuthRepository;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        SessionManager session = new SessionManager(this);

        // Nếu đã đăng nhập rồi thì quay lại luôn (không cần hiện form)
        if (session.isLoggedIn() && session.getUserId() != -1 && !session.getAuthToken().isEmpty()) {
            setResult(RESULT_OK);
            finish();
            return;
        }

        AuthRepository authRepository = new AuthRepository(this);

        EditText etEmail    = findViewById(R.id.et_email);
        EditText etPassword = findViewById(R.id.et_password);
        Button   btnLogin   = findViewById(R.id.btn_login);

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                String email    = etEmail    != null ? etEmail.getText().toString().trim()    : "";
                String password = etPassword != null ? etPassword.getText().toString().trim() : "";

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(this, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show(); return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(this, "Vui lòng nhập mật khẩu!", Toast.LENGTH_SHORT).show(); return;
                }

                btnLogin.setEnabled(false);
                authRepository.login(email, password, result -> {
                    if (result.status == Resource.Status.LOADING) {
                        btnLogin.setText("Đang đăng nhập...");
                    } else if (result.status == Resource.Status.SUCCESS) {
                        AuthResponse res = result.data;
                        if (res != null && res.user != null) {
                            session.saveUserProfile(res.user.id, res.user.fullName, res.user.email, res.token);
                            setResult(RESULT_OK);
                            finish();
                        }
                    } else if (result.status == Resource.Status.ERROR) {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Đăng nhập");
                        Toast.makeText(LoginActivity.this, result.message, Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        TextView tvRegister = findViewById(R.id.tv_register);
        if (tvRegister != null)
            tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        // Nút "Khám phá ngay" cho guest — quay lại màn hình browse
        TextView tvSkip = findViewById(R.id.tv_skip);
        if (tvSkip != null) {
            tvSkip.setOnClickListener(v -> finish());
        }
    }
}
