package com.example.cookapp.api.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RecipeMetadataResponse {

    @SerializedName("categories")
    public List<MetadataItem> categories;

    @SerializedName("dietTypes")
    public List<MetadataItem> dietTypes;

    @SerializedName("ingredients")
    public List<MetadataItem> ingredients;

    public static class MetadataItem {
        public int id;
        public String name;
        
        // Cần ghi đè toString để hiển thị tên đẹp trên Spinner Android
        @Override
        public String toString() {
            return name;
        }
    }
}
