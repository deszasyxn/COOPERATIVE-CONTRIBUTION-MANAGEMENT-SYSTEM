package com.cooperative.contribution.dto;

import com.cooperative.contribution.enumeration.MembershipStatus;

import java.time.LocalDate;

public class MemberProfileDto {

    private Long id;
    private String memberNumber;
    private String name;
    private String email;
    private String phone;
    private String address;
    private MembershipStatus membershipStatus;
    private LocalDate dateJoined;

    public MemberProfileDto() {
    }

    public MemberProfileDto(Long id, String memberNumber, String name, String email, String phone, String address, MembershipStatus membershipStatus, LocalDate dateJoined) {
        this.id = id;
        this.memberNumber = memberNumber;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.membershipStatus = membershipStatus;
        this.dateJoined = dateJoined;
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

    public LocalDate getDateJoined() {
        return dateJoined;
    }

    public void setDateJoined(LocalDate dateJoined) {
        this.dateJoined = dateJoined;
    }
}
