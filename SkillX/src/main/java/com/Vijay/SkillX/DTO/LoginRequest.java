package com.Vijay.SkillX.DTO;

public class LoginRequest {

    private String name;

    public LoginRequest() {
    }

    public LoginRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}