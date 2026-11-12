package com.cooperative.contribution.service;

import com.cooperative.contribution.dto.CooperativeSignupRequest;
import com.cooperative.contribution.dto.CooperativeSignupResponse;
import com.cooperative.contribution.dto.CompleteAdminSignupRequest;
import com.cooperative.contribution.entity.CooperativeRegistration;
import com.cooperative.contribution.entity.User;
import com.cooperative.contribution.enumeration.Role;
import com.cooperative.contribution.exception.ApiException;
import com.cooperative.contribution.repository.CooperativeRegistrationRepository;
import com.cooperative.contribution.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Service
public class CooperativeRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(CooperativeRegistrationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SETUP_TOKEN_LIFETIME_MINUTES = 30;

    private final CooperativeRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CooperativeRegistrationService(CooperativeRegistrationRepository registrationRepository,
                                          UserRepository userRepository,
                                          PasswordEncoder passwordEncoder) {
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean exists(String cooperativeName) {
        String normalizedName = normalize(cooperativeName);
        boolean exists = registrationRepository.findByNormalizedName(normalizedName)
                .map(this::hasUsableAccountOrSetup)
                .orElse(false);
        logger.debug("Cooperative registration availability checked; exists={}", exists);
        return exists;
    }

    @Transactional
    public CooperativeSignupResponse register(CooperativeSignupRequest request) {
        String name = request.getCooperativeName().trim().replaceAll("\\s+", " ");
        String normalizedName = normalize(name);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        logger.debug("Processing cooperative registration submission");
        CooperativeRegistration registration = registrationRepository.findByNormalizedName(normalizedName)
                .orElse(null);
        if (registration != null && hasUsableAccountOrSetup(registration)) {
            throw duplicateName();
        }
        if (userRepository.findByUsernameOrEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "An account already exists for this email address.");
        }

        String setupToken = generateSetupToken();
        String adminName = request.getAdminName().trim().replaceAll("\\s+", " ");
        String setupTokenHash = hashToken(setupToken);
        LocalDateTime setupExpiresAt = LocalDateTime.now().plusMinutes(SETUP_TOKEN_LIFETIME_MINUTES);
        if (registration == null) {
            registration = new CooperativeRegistration(
                    name,
                    normalizedName,
                    adminName,
                    email,
                    request.isAgreedToTerms(),
                    setupTokenHash,
                    setupExpiresAt
            );
        } else {
            registration.renewPasswordSetup(
                    name,
                    adminName,
                    email,
                    request.isAgreedToTerms(),
                    setupTokenHash,
                    setupExpiresAt
            );
        }
        try {
            CooperativeRegistration saved = registrationRepository.saveAndFlush(registration);
            logger.info("Cooperative registration request saved locally; id={}, status={}",
                    saved.getId(), saved.getStatus());
            return new CooperativeSignupResponse(
                    "PASSWORD_SETUP_REQUIRED",
                    "Registration received. Create your admin password to continue.",
                    "/login&signup/create-password.html#" + setupToken
            );
        } catch (DataIntegrityViolationException ex) {
            logger.debug("Cooperative registration insert hit a uniqueness constraint", ex);
            throw duplicateName();
        }
    }

    private boolean hasUsableAccountOrSetup(CooperativeRegistration registration) {
        return "ACTIVE".equals(registration.getStatus())
                || (registration.getSetupTokenHash() != null
                && registration.getSetupTokenExpiresAt() != null
                && registration.getSetupTokenExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Transactional
    public void completeAdminAccount(CompleteAdminSignupRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match.");
        }
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 bytes.");
        }

        String tokenHash = hashToken(request.getToken());
        CooperativeRegistration registration = registrationRepository.findBySetupTokenHash(tokenHash)
                .orElseThrow(CooperativeRegistrationService::invalidSetupToken);
        if (!"AWAITING_PASSWORD".equals(registration.getStatus())
                || registration.getSetupTokenExpiresAt() == null
                || !registration.getSetupTokenExpiresAt().isAfter(LocalDateTime.now())) {
            throw invalidSetupToken();
        }

        String email = registration.getEmail();
        if (userRepository.findByUsernameOrEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "An account already exists for this email address.");
        }

        User admin = new User(email, email, passwordEncoder.encode(request.getPassword()), Role.ROLE_ADMIN);
        try {
            userRepository.saveAndFlush(admin);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "An account already exists for this email address.");
        }

        registration.completeAccountSetup();
        registrationRepository.save(registration);
        logger.info("Admin account created for cooperative registration id={}", registration.getId());
    }

    private static String generateSetupToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private static String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    private static String normalize(String cooperativeName) {
        return cooperativeName.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static ApiException invalidSetupToken() {
        return new ApiException(HttpStatus.BAD_REQUEST, "This account setup link is invalid or has expired.");
    }

    private static ApiException duplicateName() {
        return new ApiException(HttpStatus.CONFLICT, "A cooperative with this name is already registered.");
    }
}
