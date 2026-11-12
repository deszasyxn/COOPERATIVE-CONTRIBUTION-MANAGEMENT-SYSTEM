package com.cooperative.contribution.dto;

import jakarta.validation.constraints.NotNull;

public class AdminPaymentRequest extends PaymentRequest {

    @NotNull(message = "Member ID is required")
    private Long memberId;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
}
