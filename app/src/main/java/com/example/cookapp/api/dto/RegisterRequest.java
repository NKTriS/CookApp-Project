package com.example.cookapp.api.dto;

public class RegisterRequest {
    public String email;
    public String password;
    public String fullName;
    public String phoneNumber;
    public String address;

    public RegisterRequest(String email, String password, String fullName, String phoneNumber, String address) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }
}
