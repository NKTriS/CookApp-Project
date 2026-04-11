package com.example.cookapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME     = "CookAppSession";
    private static final String KEY_USER_ID   = "USER_ID";
    private static final String KEY_LOGGED_IN = "IS_LOGGED_IN";
    private static final String KEY_USER_NAME = "USER_NAME";
    private static final String KEY_USER_EMAIL= "USER_EMAIL";
    private static final String KEY_AUTH_TOKEN= "AUTH_TOKEN"; // Trạm trung chuyển Token JWT

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref   = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /** Call this after login OR register to persist the session. */
    public void createLoginSession(int userId) {
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.apply();
    }

    /**
     * Phase 2+3: cache name & email + JWT Token
     */
    public void saveUserProfile(int userId, String name, String email, String token) {
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        if (name  != null) editor.putString(KEY_USER_NAME,  name);
        if (email != null) editor.putString(KEY_USER_EMAIL, email);
        if (token != null) editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_LOGGED_IN, false);
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    public String getCachedUserName() {
        return pref.getString(KEY_USER_NAME, "");
    }

    public String getCachedUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public String getAuthToken() {
        return pref.getString(KEY_AUTH_TOKEN, "");
    }

    public void logout() {
        editor.clear();
        editor.commit(); // synchronous — must finish before next Activity opens
    }

    /**
     * Kiểm tra đăng nhập — nếu chưa login, tự động chuyển sang LoginActivity.
     * @return true nếu CHƯA đăng nhập (caller nên return/finish)
     */
    public static boolean requireLogin(android.app.Activity activity) {
        SessionManager session = new SessionManager(activity);
        if (!session.isLoggedIn()) {
            android.widget.Toast.makeText(activity,
                "Vui lòng đăng nhập để sử dụng tính năng này!",
                android.widget.Toast.LENGTH_SHORT).show();
            activity.startActivity(new android.content.Intent(activity, LoginActivity.class));
            return true;
        }
        return false;
    }
}
