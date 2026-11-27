package com.cooperative.contribution.service;

import com.cooperative.contribution.dto.AuthResponse;
import com.cooperative.contribution.dto.LoginRequest;
import com.cooperative.contribution.entity.CooperativeRegistration;
import com.cooperative.contribution.entity.User;
import com.cooperative.contribution.enumeration.Role;
import com.cooperative.contribution.exception.ApiException;
import com.cooperative.contribution.repository.CooperativeRegistrationRepository;
import com.cooperative.contribution.repository.UserRepository;
import com.cooperative.contribution.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CooperativeRegistrationRepository cooperativeRegistrationRepository;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       CooperativeRegistrationRepository cooperativeRegistrationRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.cooperativeRegistrationRepository = cooperativeRegistrationRepository;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password."));

            return toAuthResponse(user);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }
    }

    @Transactional
    public AuthResponse getCurrentUser() {
        User authenticatedUser = SecurityUtil.getCurrentUser();
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "You are not authenticated."));
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        CooperativeRegistration registration = user.getCooperativeRegistration();
        if (registration == null && user.getRole() == Role.ROLE_ADMIN) {
            registration = cooperativeRegistrationRepository.findByEmailAndUsernameAndStatus(
                            user.getEmail(), user.getUsername(), "ACTIVE")
                    .orElse(null);
            if (registration != null) {
                user.setCooperativeRegistration(registration);
                userRepository.save(user);
            }
        }

        String redirect = user.getRole() == Role.ROLE_ADMIN
                ? "/admin/adminDash.html"
                : "/member/memberDash.html";
        String organizationName = registration == null
                ? "No Organization Assigned"
                : registration.getCooperativeName();
        return new AuthResponse(user.getUsername(), user.getEmail(), user.getRole().name(), redirect, organizationName);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
