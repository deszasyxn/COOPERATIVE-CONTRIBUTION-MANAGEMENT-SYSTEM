package com.cooperative.contribution;

import com.cooperative.contribution.repository.UserRepository;
import com.cooperative.contribution.repository.CooperativeRegistrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CooperativeSignupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CooperativeRegistrationRepository registrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanRegistrations() {
        registrationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void publicAvailabilityCheckAndRegistrationWorkWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/cooperatives/exists").param("name", "San Juan Cooperative"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));

        MvcResult signupResult = mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cooperativeName": "San Juan Cooperative",
                                  "adminName": "Alex Admin",
                                  "email": "alex@example.com",
                                  "agreedToTerms": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PASSWORD_SETUP_REQUIRED"))
                .andReturn();

        String redirect = objectMapper.readTree(signupResult.getResponse().getContentAsString())
                .get("redirect").asText();
        String setupToken = redirect.substring(redirect.indexOf('#') + 1);
        String password = "StrongPassword123";

        MvcResult accountResult = mockMvc.perform(post("/api/signup/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", setupToken,
                                "password", password,
                                "confirmPassword", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect").value("/login&signup/login.html"))
                .andReturn();

        assertNull(accountResult.getRequest().getSession(false));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", "Alex@Example.com",
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.redirect").value("/admin/adminDash.html"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alex@example.com"));

        assertTrue(userRepository.findByEmail("alex@example.com")
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .isPresent());

        mockMvc.perform(get("/api/cooperatives/exists").param("name", "  san   juan cooperative "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        mockMvc.perform(post("/api/signup/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", setupToken,
                                "password", password,
                                "confirmPassword", password
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateCooperativeRegistrationReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cooperativeName": "San Juan Cooperative",
                                  "adminName": "Alex Admin",
                                  "email": "alex@example.com",
                                  "agreedToTerms": true
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cooperativeName": " san   juan cooperative ",
                                  "adminName": "Alex Admin",
                                  "email": "alex@example.com",
                                  "agreedToTerms": true
                                }
                                """))
                .andExpect(status().isConflict());
    }
}
