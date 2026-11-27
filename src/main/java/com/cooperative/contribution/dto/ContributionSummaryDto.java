package com.cooperative.contribution.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ContributionSummaryDto {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private BigDecimal expectedContribution = ZERO;
    private BigDecimal upcomingContributions = ZERO;
    private BigDecimal overdueContributions = ZERO;
    private BigDecimal pendingContributions = ZERO;
    private BigDecimal amountPaid = ZERO;
    private BigDecimal remainingBalance = ZERO;
    private boolean hasContributionRecords;

    public BigDecimal getExpectedContribution() {
        return expectedContribution;
    }

    public void setExpectedContribution(BigDecimal expectedContribution) {
        this.expectedContribution = expectedContribution;
    }

    public BigDecimal getUpcomingContributions() {
        return upcomingContributions;
    }

    public void setUpcomingContributions(BigDecimal upcomingContributions) {
        this.upcomingContributions = upcomingContributions;
    }

    public BigDecimal getOverdueContributions() {
        return overdueContributions;
    }

    public void setOverdueContributions(BigDecimal overdueContributions) {
        this.overdueContributions = overdueContributions;
    }

    public BigDecimal getPendingContributions() {
        return pendingContributions;
    }

    public void setPendingContributions(BigDecimal pendingContributions) {
        this.pendingContributions = pendingContributions;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public boolean isHasContributionRecords() {
        return hasContributionRecords;
    }

    public void setHasContributionRecords(boolean hasContributionRecords) {
        this.hasContributionRecords = hasContributionRecords;
    }
}