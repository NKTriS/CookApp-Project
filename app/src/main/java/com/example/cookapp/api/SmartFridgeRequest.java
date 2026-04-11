package com.example.cookapp.api;

import java.util.List;

public class SmartFridgeRequest {
    public List<String> ingredients;

    public SmartFridgeRequest(List<String> ingredients) {
        this.ingredients = ingredients;
    }
}
