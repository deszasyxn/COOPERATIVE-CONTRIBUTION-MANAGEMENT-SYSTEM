package com.cooperative.contribution.controller;

import com.cooperative.contribution.dto.*;
import com.cooperative.contribution.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/members")
    public ResponseEntity<List<AdminMemberDto>> getMembers() {
        return ResponseEntity.ok(adminService.getMembers());
    }

    @PostMapping("/members")
    public ResponseEntity<AdminMemberDto> createMember(@Valid @RequestBody AdminMemberRequest request) {
        return ResponseEntity.ok(adminService.createMember(request));
    }

    @GetMapping("/members/{id}")
    public ResponseEntity<AdminMemberDto> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getMember(id));
    }

    @PutMapping("/members/{id}")
    public ResponseEntity<AdminMemberDto> updateMember(@PathVariable Long id, @Valid @RequestBody AdminMemberRequest request) {
        return ResponseEntity.ok(adminService.updateMember(id, request));
    }

    @PatchMapping("/members/{id}/status")
    public ResponseEntity<AdminMemberDto> updateMemberStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateMemberStatus(id, request));
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody AdminPaymentRequest request) {
        return ResponseEntity.ok(adminService.createPayment(request));
    }

    @PutMapping("/payments/{id}")
    public ResponseEntity<PaymentDto> updatePayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(adminService.updatePayment(id, request));
    }

    @PostMapping("/payments/{id}/confirm")
    public ResponseEntity<PaymentDto> confirmPayment(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.confirmPayment(id));
    }

    @GetMapping("/reports")
    public ResponseEntity<AdminReportDto> getReport() {
        return ResponseEntity.ok(adminService.getReport());
    }
}
