package com.cooperative.contribution.dto;

import com.cooperative.contribution.enumeration.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentDto {

    private Long id;
    private String referenceNumber;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private PaymentStatus status;

    public PaymentDto() {
    }

    public PaymentDto(Long id, String referenceNumber, String description, BigDecimal amount, LocalDate dueDate, PaymentStatus status) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.description = description;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
}
