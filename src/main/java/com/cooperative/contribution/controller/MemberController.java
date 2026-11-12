package com.cooperative.contribution.controller;

import com.cooperative.contribution.dto.*;
import com.cooperative.contribution.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member")
@PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<MemberDashboardDto> getDashboard() {
        return ResponseEntity.ok(memberService.getDashboard());
    }

    @GetMapping("/profile")
    public ResponseEntity<MemberProfileDto> getProfile() {
        return ResponseEntity.ok(memberService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<MemberProfileDto> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(memberService.updateProfile(request));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentDto>> getPayments() {
        return ResponseEntity.ok(memberService.getPayments());
    }

    @GetMapping("/payments/upcoming")
    public ResponseEntity<List<PaymentDto>> getUpcomingPayments() {
        return ResponseEntity.ok(memberService.getUpcomingPayments());
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<PaymentDto> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getPaymentById(id));
    }

    @GetMapping("/contributions")
    public ResponseEntity<List<ContributionDto>> getContributions() {
        return ResponseEntity.ok(memberService.getContributions());
    }

    @GetMapping("/contributions/summary")
    public ResponseEntity<Map<String, Object>> getContributionSummary() {
        return ResponseEntity.ok(memberService.getContributionSummary());
    }

    @GetMapping("/payment-history")
    public ResponseEntity<List<PaymentDto>> getPaymentHistory() {
        return ResponseEntity.ok(memberService.getPaymentHistory());
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications() {
        return ResponseEntity.ok(memberService.getNotifications());
    }

    @GetMapping("/notifications/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications() {
        return ResponseEntity.ok(memberService.getUnreadNotifications());
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationDto> markNotificationRead(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.markNotificationRead(id));
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllNotificationsRead() {
        memberService.markAllNotificationsRead();
        return ResponseEntity.noContent().build();
    }
}
