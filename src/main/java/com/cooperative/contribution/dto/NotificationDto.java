package com.cooperative.contribution.dto;

import com.cooperative.contribution.enumeration.NotificationType;

import java.time.LocalDateTime;

public class NotificationDto {

    private Long id;
    private String title;
    private String message;
    private NotificationType notificationType;
    private boolean read;
    private LocalDateTime createdDate;

    public NotificationDto() {
    }

    public NotificationDto(Long id, String title, String message, NotificationType notificationType, boolean read, LocalDateTime createdDate) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.notificationType = notificationType;
        this.read = read;
        this.createdDate = createdDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
