package com.cooperative.contribution.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CooperativeSignupRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    @Pattern(regexp = "^[\\p{L}\\p{N} .,'&()\\-]+$")
    private String cooperativeName;

    @NotBlank
    @Size(min = 2, max = 80)
    @Pattern(regexp = "^[\\p{L} .'\\-]+$")
    private String adminName;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @AssertTrue(message = "You must accept the terms and conditions.")
    private boolean agreedToTerms;

    public String getCooperativeName() {
        return cooperativeName;
    }

    public void setCooperativeName(String cooperativeName) {
        this.cooperativeName = cooperativeName;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAgreedToTerms() {
        return agreedToTerms;
    }

    public void setAgreedToTerms(boolean agreedToTerms) {
        this.agreedToTerms = agreedToTerms;
    }
}
