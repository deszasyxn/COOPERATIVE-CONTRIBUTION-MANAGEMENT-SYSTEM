package com.cooperative.contribution.dto;

public class AuthResponse {

    private String username;
    private String email;
    private String role;
    private String redirect;

    public AuthResponse() {
    }

    public AuthResponse(String username, String email, String role, String redirect) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.redirect = redirect;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }
}
