package com.cooperative.contribution.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cooperative_registrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = "normalized_name")
})
public class CooperativeRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cooperative_name", nullable = false, length = 100)
    private String cooperativeName;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 100)
    private String normalizedName;

    @Column(name = "admin_name", nullable = false, length = 80)
    private String adminName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "terms_accepted", nullable = false)
    private boolean termsAccepted;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "setup_token_hash", unique = true, length = 64)
    private String setupTokenHash;

    @Column(name = "setup_token_expires_at")
    private LocalDateTime setupTokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected CooperativeRegistration() {
    }

    public CooperativeRegistration(String cooperativeName, String normalizedName, String adminName,
                                  String email, boolean termsAccepted, String setupTokenHash,
                                  LocalDateTime setupTokenExpiresAt) {
        this.cooperativeName = cooperativeName;
        this.normalizedName = normalizedName;
        this.adminName = adminName;
        this.email = email;
        this.termsAccepted = termsAccepted;
        this.status = "AWAITING_PASSWORD";
        this.setupTokenHash = setupTokenHash;
        this.setupTokenExpiresAt = setupTokenExpiresAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCooperativeName() {
        return cooperativeName;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }

    public String getStatus() {
        return status;
    }

    public String getSetupTokenHash() {
        return setupTokenHash;
    }

    public LocalDateTime getSetupTokenExpiresAt() {
        return setupTokenExpiresAt;
    }

    public void renewPasswordSetup(String cooperativeName, String adminName, String email,
                                   boolean termsAccepted, String setupTokenHash,
                                   LocalDateTime setupTokenExpiresAt) {
        this.cooperativeName = cooperativeName;
        this.adminName = adminName;
        this.email = email;
        this.termsAccepted = termsAccepted;
        this.status = "AWAITING_PASSWORD";
        this.setupTokenHash = setupTokenHash;
        this.setupTokenExpiresAt = setupTokenExpiresAt;
    }

    public void completeAccountSetup() {
        this.status = "ACTIVE";
        this.setupTokenHash = null;
        this.setupTokenExpiresAt = null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
