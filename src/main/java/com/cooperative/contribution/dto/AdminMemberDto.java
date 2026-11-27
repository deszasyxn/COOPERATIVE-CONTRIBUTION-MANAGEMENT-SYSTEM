package com.cooperative.contribution.dto;

import com.cooperative.contribution.enumeration.MembershipStatus;

import java.math.BigDecimal;

public class AdminMemberDto {

    private Long id;
    private String memberNumber;
    private String name;
    private String email;
    private String phone;
    private String address;
    private MembershipStatus membershipStatus;
    private String organizationName;
    private BigDecimal expectedContribution = BigDecimal.ZERO;
    private BigDecimal upcomingContributions = BigDecimal.ZERO;
    private BigDecimal overdueContributions = BigDecimal.ZERO;
    private BigDecimal pendingContributions = BigDecimal.ZERO;
    private BigDecimal amountPaid = BigDecimal.ZERO;
    private BigDecimal remainingBalance = BigDecimal.ZERO;
    private boolean hasContributionRecords;

    public AdminMemberDto() {
    }

    public AdminMemberDto(Long id, String memberNumber, String name, String email, String phone, String address, MembershipStatus membershipStatus) {
        this(id, memberNumber, name, email, phone, address, membershipStatus, "No Organization Assigned");
    }

    public AdminMemberDto(Long id, String memberNumber, String name, String email, String phone, String address,
                          MembershipStatus membershipStatus, String organizationName) {
        this.id = id;
        this.memberNumber = memberNumber;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.membershipStatus = membershipStatus;
        this.organizationName = organizationName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMemberNumber() {
        return memberNumber;
    }

    public void setMemberNumber(String memberNumber) {
        this.memberNumber = memberNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public MembershipStatus getMembershipStatus() {
        return membershipStatus;
    }

    public void setMembershipStatus(MembershipStatus membershipStatus) {
        this.membershipStatus = membershipStatus;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

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
