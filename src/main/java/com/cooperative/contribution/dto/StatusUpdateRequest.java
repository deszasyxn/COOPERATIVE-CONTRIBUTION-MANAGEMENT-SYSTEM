package com.cooperative.contribution.dto;

import com.cooperative.contribution.enumeration.MembershipStatus;
import com.cooperative.contribution.enumeration.PaymentStatus;

public class StatusUpdateRequest {

    private MembershipStatus membershipStatus;
    private PaymentStatus paymentStatus;

    public MembershipStatus getMembershipStatus() {
        return membershipStatus;
    }

    public void setMembershipStatus(MembershipStatus membershipStatus) {
        this.membershipStatus = membershipStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
