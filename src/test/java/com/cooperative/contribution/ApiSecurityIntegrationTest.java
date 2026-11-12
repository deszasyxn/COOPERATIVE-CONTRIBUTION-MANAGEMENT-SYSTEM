package com.cooperative.contribution;

import com.cooperative.contribution.entity.Member;
import com.cooperative.contribution.entity.Payment;
import com.cooperative.contribution.entity.User;
import com.cooperative.contribution.entity.Contribution;
import com.cooperative.contribution.entity.Notification;
import com.cooperative.contribution.enumeration.MembershipStatus;
import com.cooperative.contribution.enumeration.NotificationType;
import com.cooperative.contribution.enumeration.PaymentStatus;
import com.cooperative.contribution.enumeration.Role;
import com.cooperative.contribution.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ContributionRepository contributionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        contributionRepository.deleteAll();
        paymentRepository.deleteAll();
        memberRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User("admin", "admin@example.com", passwordEncoder.encode("secret123"), Role.ROLE_ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);

        User memberUser = new User("member", "member@example.com", passwordEncoder.encode("secret123"), Role.ROLE_MEMBER);
        memberUser.setEnabled(true);
        userRepository.save(memberUser);

        Member member = new Member("M-1001", "Member User", memberUser);
        member.setMembershipStatus(MembershipStatus.ACTIVE);
        memberRepository.save(member);

        Payment payment = new Payment(member, "PAY-1001", "Monthly contribution", new BigDecimal("250.00"), LocalDate.now().plusDays(5), PaymentStatus.SCHEDULED);
        paymentRepository.save(payment);

        Contribution contribution = new Contribution(member, new BigDecimal("250.00"), LocalDateTime.now().minusDays(2), "Cash", "CON-1001");
        contributionRepository.save(contribution);

        Notification notification = new Notification(member, "Welcome", "Welcome to your member portal.", NotificationType.INFORMATION);
        notificationRepository.save(notification);
    }

    @Test
    void loginAndDashboardForMemberShouldReturnDashboardData() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"member\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_MEMBER"))
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(get("/api/member/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberName").value("Member User"))
                .andExpect(jsonPath("$.memberNumber").value("M-1001"))
                .andExpect(jsonPath("$.totalContributions").value(250.00));
    }

    @Test
    void adminCanViewMembersAndMemberDataIsolationIsEnforced() throws Exception {
        User secondMemberUser = new User("member2", "member2@example.com", passwordEncoder.encode("secret123"), Role.ROLE_MEMBER);
        userRepository.save(secondMemberUser);

        Member secondMember = new Member("M-1002", "Second Member", secondMemberUser);
        memberRepository.save(secondMember);

        Payment secondPayment = new Payment(secondMember, "PAY-1002", "Another payment", new BigDecimal("400.00"), LocalDate.now().plusDays(10), PaymentStatus.UPCOMING);
        paymentRepository.save(secondPayment);

        MockHttpSession adminSession = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(get("/api/admin/members").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        MockHttpSession memberSession = (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"member\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(get("/api/member/payments/{id}", secondPayment.getId()).session(memberSession))
                .andExpect(status().isForbidden());
    }
}
