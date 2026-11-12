package com.cooperative.contribution.controller;

import com.cooperative.contribution.dto.CompleteAdminSignupRequest;
import com.cooperative.contribution.dto.CooperativeSignupRequest;
import com.cooperative.contribution.dto.CooperativeSignupResponse;
import com.cooperative.contribution.service.CooperativeRegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Validated
public class CooperativeRegistrationController {

    private final CooperativeRegistrationService registrationService;

    public CooperativeRegistrationController(CooperativeRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/api/cooperatives/exists")
    public ResponseEntity<Map<String, Boolean>> cooperativeExists(
            @RequestParam @NotBlank @Size(max = 100) String name) {
        return ResponseEntity.ok(Map.of("exists", registrationService.exists(name)));
    }

    @PostMapping("/api/signup")
    public ResponseEntity<CooperativeSignupResponse> register(
            @Valid @RequestBody CooperativeSignupRequest request) {
        return ResponseEntity.status(201).body(registrationService.register(request));
    }

    @PostMapping("/api/signup/complete")
    public ResponseEntity<Map<String, String>> completeAdminSignup(
            @Valid @RequestBody CompleteAdminSignupRequest request) {
        registrationService.completeAdminAccount(request);
        return ResponseEntity.ok(Map.of(
                "message", "Admin account created. Log in with your email and new password.",
                "redirect", "/login&signup/login.html"
        ));
    }
}
