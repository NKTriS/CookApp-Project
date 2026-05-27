package com.example.cookapp.utils;

import android.os.Build;

public class NetworkConfig {
    // 1. IP mạng cục bộ cho máy ảo Android Emulator
    private static final String EMULATOR_IP = "http://10.0.2.2:3000";

    // 2. IP LAN của máy tính dùng để test trên MÁY THẬT qua Wifi
    // (Đổi IP này nếu bạn kết nối Wifi khác)
    public static final String LAN_IP = "http://192.168.111.23:3000";

    public static String getBaseUrl() {
        boolean emu = isEmulator();
        android.util.Log.d("NetworkConfig", "isEmulator(): " + emu);
        android.util.Log.d("NetworkConfig", "FINGERPRINT: " + android.os.Build.FINGERPRINT);
        android.util.Log.d("NetworkConfig", "MODEL: " + android.os.Build.MODEL);
        android.util.Log.d("NetworkConfig", "MANUFACTURER: " + android.os.Build.MANUFACTURER);
        android.util.Log.d("NetworkConfig", "HARDWARE: " + android.os.Build.HARDWARE);
        if (emu) {
            android.util.Log.d("NetworkConfig", "Using EMULATOR_IP: " + EMULATOR_IP);
            return EMULATOR_IP;
        } else {
            android.util.Log.d("NetworkConfig", "Using LAN_IP: " + LAN_IP);
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
                || "google_sdk".equals(Build.PRODUCT)
                || Build.FINGERPRINT.contains("sdk_gphone")
                || Build.MODEL.contains("sdk_gphone")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu");
    }
}
