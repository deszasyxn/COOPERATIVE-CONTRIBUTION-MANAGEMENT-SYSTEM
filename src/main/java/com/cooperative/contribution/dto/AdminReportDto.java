package com.cooperative.contribution.dto;

import java.math.BigDecimal;

public class AdminReportDto {

    private long totalMembers;
    private long upcomingPayments;
    private BigDecimal totalContributions;
    private BigDecimal totalCollected;

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
}
