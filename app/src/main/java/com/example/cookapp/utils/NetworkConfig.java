package com.example.cookapp.utils;

import android.os.Build;

public class NetworkConfig {
    // 1. IP mạng cục bộ cho máy ảo Android Emulator
    private static final String EMULATOR_IP = "http://10.0.2.2:3000";

    // 2. IP LAN của máy tính dùng để test trên MÁY THẬT qua Wifi
    // (Đổi IP này nếu bạn kết nối Wifi khác)
    public static final String LAN_IP = "http://172.11.142.10:3000";

    public static String getBaseUrl() {
        if (isEmulator()) {
            return EMULATOR_IP;
        } else {
            return LAN_IP;
        }
    }

    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}
