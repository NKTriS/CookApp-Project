package com.example.cookapp.api.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CheckIngredientsRequest {
    @SerializedName("names")
    public List<String> names;

    public CheckIngredientsRequest(List<String> names) {
        this.names = names;
    }
}
