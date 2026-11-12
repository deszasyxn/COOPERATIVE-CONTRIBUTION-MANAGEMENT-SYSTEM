package com.cooperative.contribution.service;

import com.cooperative.contribution.dto.AuthResponse;
import com.cooperative.contribution.dto.LoginRequest;
import com.cooperative.contribution.entity.User;
import com.cooperative.contribution.exception.ApiException;
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

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsernameOrEmail(request.getUsername())
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password."));

            String redirect = user.getRole().name().equals("ROLE_ADMIN")
                    ? "/admin/adminDash.html"
                    : "/member/memberDash.html";

            return new AuthResponse(user.getUsername(), user.getEmail(), user.getRole().name(), redirect);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }
    }

    public AuthResponse getCurrentUser() {
        User user = SecurityUtil.getCurrentUser();
        return new AuthResponse(user.getUsername(), user.getEmail(), user.getRole().name(),
                user.getRole().name().equals("ROLE_ADMIN") ? "/admin/adminDash.html" : "/member/memberDash.html");
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
