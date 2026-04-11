package com.example.cookapp.utils.image;

import java.util.HashMap;
import java.util.Map;

public class ImageResolver {

    private static final Map<String, String> ALIAS_MAP = new HashMap<>();

    static {
        // Map common variations to the master key used in Curated maps
        ALIAS_MAP.put("hanh la", "hanh la");
        ALIAS_MAP.put("scallion", "hanh la");
        ALIAS_MAP.put("green onion", "hanh la");
        
        ALIAS_MAP.put("thit bo bam", "thit bo bam");
        ALIAS_MAP.put("ground beef", "thit bo bam");
        ALIAS_MAP.put("minced beef", "thit bo bam");

        ALIAS_MAP.put("banh mi", "banh mi");
        ALIAS_MAP.put("baguette", "banh mi");
        ALIAS_MAP.put("vietnamese bread", "banh mi");

        ALIAS_MAP.put("ca chua", "ca chua");
        ALIAS_MAP.put("tomato", "ca chua");

        ALIAS_MAP.put("tom su", "tom su");
        ALIAS_MAP.put("shrimp", "tom su");
        ALIAS_MAP.put("tiger prawn", "tom su");
    }

    /**
     * Resolves the verified semantic URL for an ingredient.
     */
    public static String resolveIngredientImage(String rawName) {
        String normalized = StringUtils.normalizeName(rawName);
        
        // 1. Resolve Alias
        if (ALIAS_MAP.containsKey(normalized)) {
            normalized = ALIAS_MAP.get(normalized);
        }

        // 2. Lookup in curated map
        return CuratedIngredientImages.getUrl(normalized);
    }

    /**
     * Resolves the verified semantic URL for a recipe.
     */
    public static String resolveRecipeImage(String rawTitle) {
        String normalized = StringUtils.normalizeName(rawTitle);
        // Direct lookup; recipes usually don't have multi-lingual aliases in this context
        return CuratedRecipeImages.getUrl(normalized);
    }
}
