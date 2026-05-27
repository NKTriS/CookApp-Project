package com.example.cookapp;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.RetrofitClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình Chef AI Bot.
 *
 * Lớp này quản lý giao diện chat, lưu lịch sử hội thoại tạm thời trên màn hình,
 * đóng gói tin nhắn thành JSON và gọi API nội bộ POST /api/chat. Backend sau đó
 * bổ sung ngữ cảnh công thức từ CSDL và gọi dịch vụ Groq để sinh câu trả lời.
 */
public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageView btnSend;
    private ProgressBar progressLoading;
    private ChatAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        // AI Chat yêu cầu đăng nhập
        if (SessionManager.requireLogin(this)) { finish(); return; }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(16, systemBars.top + 16, 16, 16);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_input), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(12, 12, 12, systemBars.bottom + 12);
            return insets;
        });

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        rvChat = findViewById(R.id.rv_chat);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        progressLoading = findViewById(R.id.progress_loading);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        rvChat.setLayoutManager(lm);
        adapter = new ChatAdapter(messages);
        rvChat.setAdapter(adapter);

        // Tin nhắn chào mừng giúp người dùng biết Chef AI hỗ trợ các câu hỏi về nấu ăn.
        messages.add(new ChatMessage("Xin chào! Tôi là Chef AI 👨‍🍳\nTôi có thể giúp bạn:\n\n🍳 Gợi ý món ăn phù hợp\n🧊 Tìm món từ nguyên liệu có sẵn\n📊 Phân tích dinh dưỡng\n\nHãy hỏi tôi bất cứ điều gì!", ChatMessage.TYPE_BOT));
        adapter.notifyItemInserted(0);

        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    /**
     * Gửi một tin nhắn của người dùng tới Chef AI.
     *
     * Hàm này thêm tin nhắn lên RecyclerView, tạo JSON gồm message và history,
     * sau đó gọi Retrofit API sendChatMessage(). Kết quả trả về được parse từ
     * trường reply và thêm vào danh sách tin nhắn dạng phản hồi của bot.
     */
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        // Ẩn bàn phím để người dùng nhìn rõ trạng thái gửi tin nhắn.
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etMessage.getWindowToken(), 0);

        // Thêm tin nhắn người dùng vào danh sách trước để giao diện phản hồi ngay.
        messages.add(new ChatMessage(text, ChatMessage.TYPE_USER));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
        etMessage.setText("");

        // Hiển thị loading và khóa nút gửi trong lúc chờ backend trả lời.
        progressLoading.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        // Tạo request body JSON đúng định dạng API: message + history.
        try {
            JSONObject body = new JSONObject();
            body.put("message", text);

            // Gửi kèm lịch sử hội thoại, bỏ qua tin nhắn chào mừng mặc định.
            JSONArray historyArr = new JSONArray();
            for (int i = 1; i < messages.size() - 1; i++) {
                ChatMessage m = messages.get(i);
                JSONObject h = new JSONObject();
                h.put("role", m.getRole());
                h.put("content", m.getContent());
                historyArr.put(h);
            }
            body.put("history", historyArr);

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"), body.toString());

            // Gọi API nội bộ POST /api/chat; token đăng nhập được RetrofitClient tự gắn vào header.
            ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
            api.sendChatMessage(requestBody).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    progressLoading.setVisibility(View.GONE);
                    btnSend.setEnabled(true);

                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String json = response.body().string();
                            JSONObject obj = new JSONObject(json);
                            String reply = obj.getString("reply");

                            messages.add(new ChatMessage(reply, ChatMessage.TYPE_BOT));
                            adapter.notifyItemInserted(messages.size() - 1);
                            rvChat.scrollToPosition(messages.size() - 1);
                        } else {
                            showError("Lỗi từ máy chủ");
                        }
                    } catch (Exception e) {
                        showError("Lỗi xử lý phản hồi");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    progressLoading.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    showError("Lỗi mạng: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            progressLoading.setVisibility(View.GONE);
            btnSend.setEnabled(true);
            showError("Lỗi tạo yêu cầu");
        }
    }

    /**
     * Hiển thị lỗi dưới dạng một tin nhắn của bot để người dùng biết thao tác thất bại.
     */
    private void showError(String msg) {
        messages.add(new ChatMessage("❌ " + msg + "\nVui lòng thử lại!", ChatMessage.TYPE_BOT));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
    }
}
