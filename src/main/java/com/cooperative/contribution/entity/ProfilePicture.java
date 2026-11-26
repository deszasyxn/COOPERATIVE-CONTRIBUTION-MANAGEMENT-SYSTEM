package com.cooperative.contribution.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "profile_pictures")
public class ProfilePicture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Lob
    @Column(nullable = false)
    private byte[] data;

    @Column(nullable = false, length = 32)
    private String contentType;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    @PreUpdate
    protected void onUpload() {
        this.uploadedAt = LocalDateTime.now();
    }

    public ProfilePicture() {
    }

    public ProfilePicture(User user, byte[] data, String contentType) {
        this.user = user;
        this.data = data;
        this.contentType = contentType;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}