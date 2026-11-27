package com.cooperative.contribution.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class MemberDashboardDto {

    private String memberName;
    private String memberNumber;
    private String organizationName = "No Organization Assigned";
    private BigDecimal totalContributions = BigDecimal.ZERO;
    private BigDecimal expectedContribution = BigDecimal.ZERO;
    private BigDecimal amountPaid = BigDecimal.ZERO;
    private BigDecimal remainingBalance = BigDecimal.ZERO;
    private BigDecimal nextPaymentAmount = BigDecimal.ZERO;
    private LocalDate nextPaymentDueDate;
    private long completedPayments;
    private long scheduledPayments;
    private String accountStanding;
    private List<PaymentDto> upcomingPayments;
    private List<ContributionDto> recentActivities;
    private Map<String, BigDecimal> monthlyContributionStats;

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMemberNumber() {
        return memberNumber;
    }

    public void setMemberNumber(String memberNumber) {
        this.memberNumber = memberNumber;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public BigDecimal getTotalContributions() {
        return totalContributions;
    }

    public void setTotalContributions(BigDecimal totalContributions) {
        this.totalContributions = totalContributions;
        this.amountPaid = totalContributions;
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
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public BigDecimal getNextPaymentAmount() {
        return nextPaymentAmount;
    }

    public void setNextPaymentAmount(BigDecimal nextPaymentAmount) {
        this.nextPaymentAmount = nextPaymentAmount;
    }

    public LocalDate getNextPaymentDueDate() {
        return nextPaymentDueDate;
    }

    public void setNextPaymentDueDate(LocalDate nextPaymentDueDate) {
        this.nextPaymentDueDate = nextPaymentDueDate;
    }

    public long getCompletedPayments() {
        return completedPayments;
    }

    public void setCompletedPayments(long completedPayments) {
        this.completedPayments = completedPayments;
    }

    public long getScheduledPayments() {
        return scheduledPayments;
    }

    public void setScheduledPayments(long scheduledPayments) {
        this.scheduledPayments = scheduledPayments;
    }

    public String getAccountStanding() {
        return accountStanding;
    }

    public void setAccountStanding(String accountStanding) {
        this.accountStanding = accountStanding;
    }

    public List<PaymentDto> getUpcomingPayments() {
        return upcomingPayments;
    }

    public void setUpcomingPayments(List<PaymentDto> upcomingPayments) {
        this.upcomingPayments = upcomingPayments;
    }

    public List<ContributionDto> getRecentActivities() {
        return recentActivities;
    }

    public void setRecentActivities(List<ContributionDto> recentActivities) {
        this.recentActivities = recentActivities;
    }

    public Map<String, BigDecimal> getMonthlyContributionStats() {
        return monthlyContributionStats;
    }

    public void setMonthlyContributionStats(Map<String, BigDecimal> monthlyContributionStats) {
        this.monthlyContributionStats = monthlyContributionStats;
    }
}
