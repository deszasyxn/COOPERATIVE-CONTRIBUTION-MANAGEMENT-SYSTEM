package com.cooperative.contribution.dto;

import com.cooperative.contribution.enumeration.MembershipStatus;

public class AdminMemberDto {

    private Long id;
    private String memberNumber;
    private String name;
    private String email;
    private String phone;
    private MembershipStatus membershipStatus;

    public AdminMemberDto() {
    }

    public AdminMemberDto(Long id, String memberNumber, String name, String email, String phone, MembershipStatus membershipStatus) {
        this.id = id;
        this.memberNumber = memberNumber;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.membershipStatus = membershipStatus;
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

    public MembershipStatus getMembershipStatus() {
        return membershipStatus;
    }

    public void setMembershipStatus(MembershipStatus membershipStatus) {
        this.membershipStatus = membershipStatus;
    }
}
