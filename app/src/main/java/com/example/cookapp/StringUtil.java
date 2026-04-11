package com.example.cookapp;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class StringUtil {
    
    // Normalizes Vietnamese accents so text can be searched efficiently
    public static String removeAccent(String s) {
        if (s == null) return "";
        String temp = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    public static boolean containsIgnoreCaseAndAccents(String text, String query) {
        if (text == null || query == null) return false;
        String normalizedText = removeAccent(text).toLowerCase();
        String normalizedQuery = removeAccent(query).toLowerCase();
        return normalizedText.contains(normalizedQuery);
    }
}
