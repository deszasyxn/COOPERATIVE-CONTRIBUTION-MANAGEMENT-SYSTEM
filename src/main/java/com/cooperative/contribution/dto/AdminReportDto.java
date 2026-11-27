package com.cooperative.contribution.dto;

import java.math.BigDecimal;

public class AdminReportDto {

    private long totalMembers;
    private long upcomingPayments;
    private BigDecimal totalContributions;
    private BigDecimal totalCollected;
    private BigDecimal expectedContribution = BigDecimal.ZERO;
    private BigDecimal amountPaid = BigDecimal.ZERO;
    private BigDecimal remainingBalance = BigDecimal.ZERO;

    public AdminReportDto() {
    }

    public AdminReportDto(long totalMembers, long upcomingPayments, BigDecimal totalContributions, BigDecimal totalCollected) {
        this.totalMembers = totalMembers;
        this.upcomingPayments = upcomingPayments;
        this.totalContributions = totalContributions;
        this.totalCollected = totalCollected;
    }

    public long getTotalMembers() {
        return totalMembers;
    }

    public void setTotalMembers(long totalMembers) {
        this.totalMembers = totalMembers;
    }

    public long getUpcomingPayments() {
        return upcomingPayments;
    }

    public void setUpcomingPayments(long upcomingPayments) {
        this.upcomingPayments = upcomingPayments;
    }

    public BigDecimal getTotalContributions() {
        return totalContributions;
    }

    public void setTotalContributions(BigDecimal totalContributions) {
        this.totalContributions = totalContributions;
    }

    public BigDecimal getTotalCollected() {
        return totalCollected;
    }

    public void setTotalCollected(BigDecimal totalCollected) {
        this.totalCollected = totalCollected;
    }

    public BigDecimal getExpectedContribution() {
        return expectedContribution;
    }

    public void setExpectedContribution(BigDecimal expectedContribution) {
        this.expectedContribution = expectedContribution;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
        this.totalContributions = amountPaid;
        this.totalCollected = amountPaid;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }
}
