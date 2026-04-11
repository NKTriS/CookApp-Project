package com.example.cookapp.utils.image;

import java.text.Normalizer;
import java.util.Locale;

public class StringUtils {
    /**
     * Normalizes a string for easy comparison and matching:
     * - Trims whitespace
     * - Converts to lowercase
     * - Removes redundant spaces
     * - Removes Vietnamese diacritics (accents)
     * e.g., "Bún bò Huế " -> "bun bo hue"
     */
    public static String normalizeName(String input) {
        if (input == null) return "";
        
        // Trim and lowercase
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        
        // Remove accents (diacritics)
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        
        // Edge cases for Vietnamese 'đ' -> 'd' which Normalizer.NFD sometimes misses
        normalized = normalized.replace("đ", "d");
        
        // Replace multiple spaces with a single space
        normalized = normalized.replaceAll("\\s+", " ");
        
        return normalized;
    }
}
