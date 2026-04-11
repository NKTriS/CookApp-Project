package com.example.cookapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cookapp.api.ApiService;
import com.example.cookapp.api.RetrofitClient;
import com.example.cookapp.api.dto.VnpayUrlResponse;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VnpayPaymentActivity extends AppCompatActivity {

    private WebView webView;
    private LinearLayout llLoading;
    
    private int orderId;
    private long orderTotal;
    private String customerName, address, itemsSummary;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_payment);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), sys.top, v.getPaddingRight(), sys.bottom);
            return insets;
        });

        orderId = getIntent().getIntExtra("order_id", -1);
        orderTotal = getIntent().getLongExtra("order_total", 0);
        customerName = getIntent().getStringExtra("customer_name");
        address = getIntent().getStringExtra("address");
        itemsSummary = getIntent().getStringExtra("items_summary");

        ImageView ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> onBackPressed());
        }

        webView = findViewById(R.id.webview);
        llLoading = findViewById(R.id.ll_loading);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        webView.setWebChromeClient(new android.webkit.WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d("VNPay", "Loading: " + url);
                if (url.contains("/api/payment/vnpay/return")) {
                    handleReturnUrl(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                android.widget.Button btnFillTest = findViewById(R.id.btn_fill_test);
                if (url != null && url.contains("sandbox.vnpayment.vn")) {
                    btnFillTest.setVisibility(View.VISIBLE);
                } else {
                    btnFillTest.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("VNPay", "WebView error: " + errorCode + " - " + description + " URL: " + failingUrl);
                Toast.makeText(VnpayPaymentActivity.this, "Lỗi tải trang: " + description, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                Log.e("VNPay", "SSL error: " + error.toString());
                handler.proceed(); // Cho phép SSL sandbox
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.contains("/api/payment/vnpay/return")) {
                    handleReturnUrl(url);
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }
        });

        android.widget.Button btnFillTest = findViewById(R.id.btn_fill_test);
        btnFillTest.setOnClickListener(v -> {
            String js = "javascript:(function() {" +
                    "var e = new Event('input', { bubbles: true });" +
                    "var c = document.getElementById('card_number_mask'); if(c) { c.value = '9704198526191432198'; c.dispatchEvent(e); }" +
                    "var n = document.getElementById('cardHolder'); if(n) { n.value = 'NGUYEN VAN A'; n.dispatchEvent(e); }" +
                    "var d = document.getElementById('cardDate'); if(d) { d.value = '07/15'; d.dispatchEvent(e); }" +
                    "})();";
            webView.evaluateJavascript(js, null);
            Toast.makeText(this, "Đã điền tự động!", Toast.LENGTH_SHORT).show();
        });

        fetchPaymentUrl();
    }

    private void fetchPaymentUrl() {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("amount", orderTotal);
        body.put("bankCode", ""); // Empty leaves it to user choice on VNPay portal

        RetrofitClient.getClient(this).create(ApiService.class)
                .createVnpayUrl(body)
                .enqueue(new Callback<VnpayUrlResponse>() {
                    @Override
                    public void onResponse(Call<VnpayUrlResponse> call, Response<VnpayUrlResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String url = response.body().paymentUrl;
                            webView.loadUrl(url);
                            llLoading.setVisibility(View.GONE);
                            webView.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(VnpayPaymentActivity.this, "Lỗi tạo URL thanh toán", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<VnpayUrlResponse> call, Throwable t) {
                        Toast.makeText(VnpayPaymentActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void handleReturnUrl(String url) {
        Uri uri = Uri.parse(url);
        String vnpResponseCode = uri.getQueryParameter("vnp_ResponseCode");

        // Báo cho backend biết kết quả bằng cách tự động call URL return dưới nền.
        // Vì WebView bị chặn (intercept) và đóng lại ngay lập tức nên request chưa kịp tới backend.
        new Thread(() -> {
            try {
                java.net.URL backendUrl = new java.net.URL(url);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) backendUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.getResponseCode(); // Gọi để backend chạy code xử lý đổi trạng thái
            } catch (Exception e) {
                Log.e("VNPay", "Error triggering backend return URL", e);
            }
        }).start();

        if ("00".equals(vnpResponseCode)) {
            // Thanh toán thành công
            Intent intent = new Intent(VnpayPaymentActivity.this, OrderSuccessActivity.class);
            intent.putExtra("order_id", orderId);
            intent.putExtra("is_remote", true);
            intent.putExtra("order_total", orderTotal);
            intent.putExtra("payment_method", "VNPay");
            intent.putExtra("customer_name", customerName);
            intent.putExtra("address", address);
            intent.putExtra("items_summary", itemsSummary);
            startActivity(intent);
            finish();
        } else {
            // Lỗi hoặc người dùng hủy
            Toast.makeText(this, "Thanh toán bị hủy hoặc lỗi (Mã: " + vnpResponseCode + ")", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        // Nếu người dùng bấm back, ta coi như hủy thanh toán
        super.onBackPressed();
        new Thread(() -> {
            try {
                Map<String, String> body = new HashMap<>();
                body.put("reason", "Người dùng hủy thanh toán VNPay");
                RetrofitClient.getClient(this)
                    .create(ApiService.class)
                    .cancelOrder(orderId, body)
                    .execute();
            } catch (Exception e) { Log.w("VnpayPayment", "Failed to cancel order on back", e); }
        }).start();
        Toast.makeText(this, "Đã hủy thanh toán", Toast.LENGTH_SHORT).show();
    }
}
