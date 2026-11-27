package com.cooperative.contribution;

import com.cooperative.contribution.entity.Member;
import com.cooperative.contribution.entity.Payment;
import com.cooperative.contribution.entity.User;
import com.cooperative.contribution.entity.Contribution;
import com.cooperative.contribution.entity.CooperativeRegistration;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private CooperativeRegistrationRepository cooperativeRegistrationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProfilePictureRepository profilePictureRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        profilePictureRepository.deleteAll();
        contributionRepository.deleteAll();
        paymentRepository.deleteAll();
        memberRepository.deleteAll();
        userRepository.deleteAll();
        cooperativeRegistrationRepository.deleteAll();

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
                .andExpect(jsonPath("$.organizationName").value("No Organization Assigned"))
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(get("/api/member/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberName").value("Member User"))
                .andExpect(jsonPath("$.memberNumber").value("M-1001"))
                .andExpect(jsonPath("$.organizationName").value("No Organization Assigned"))
                .andExpect(jsonPath("$.expectedContribution").value(250.00))
                .andExpect(jsonPath("$.amountPaid").value(250.00))
                .andExpect(jsonPath("$.remainingBalance").value(0))
                .andExpect(jsonPath("$.totalContributions").value(250.00));
    }

    @Test
    void memberAndAdminUseTheSameDuePaidAndBalanceCalculations() throws Exception {
        contributionRepository.deleteAll();
        paymentRepository.deleteAll();
        Member member = memberRepository.findByMemberNumber("M-1001").orElseThrow();
        LocalDate dueDate = LocalDate.now().minusDays(1);

        Payment paidFirst = new Payment(member, "CALC-PAID-1", "First installment", new BigDecimal("100.00"), dueDate, PaymentStatus.PAID);
        Payment paidSecond = new Payment(member, "CALC-PAID-2", "Second installment", new BigDecimal("50.00"), dueDate, PaymentStatus.PAID);
        Payment pending = new Payment(member, "CALC-PENDING", "Pending transaction", new BigDecimal("25.00"), dueDate, PaymentStatus.PENDING);
        Payment rejected = new Payment(member, "CALC-REJECTED", "Rejected transaction", new BigDecimal("30.00"), dueDate, PaymentStatus.REJECTED);
        Payment cancelled = new Payment(member, "CALC-CANCELLED", "Cancelled transaction", new BigDecimal("40.00"), dueDate, PaymentStatus.CANCELLED);
        Payment partialSchedule = new Payment(member, "CALC-PARTIAL", "Partial installment", new BigDecimal("100.00"), dueDate, PaymentStatus.SCHEDULED);
        Payment future = new Payment(member, "CALC-FUTURE", "Future contribution", new BigDecimal("300.00"), LocalDate.now().plusDays(10), PaymentStatus.SCHEDULED);
        paymentRepository.saveAll(java.util.List.of(paidFirst, paidSecond, pending, rejected, cancelled, partialSchedule, future));

        Contribution paidMirror = new Contribution(member, new BigDecimal("100.00"), LocalDateTime.now(), "System", "CALC-MIRROR");
        paidMirror.setPayment(paidFirst);
        Contribution partial = new Contribution(member, new BigDecimal("40.00"), LocalDateTime.now(), "Cash", "CALC-PARTIAL-TX");
        partial.setPayment(partialSchedule);
        Contribution pendingTransaction = new Contribution(member, new BigDecimal("25.00"), LocalDateTime.now(), "Cash", "CALC-PENDING-TX");
        pendingTransaction.setPayment(pending);
        contributionRepository.saveAll(java.util.List.of(paidMirror, partial, pendingTransaction));

        MockHttpSession memberSession = login("member");
        mockMvc.perform(get("/api/member/dashboard").session(memberSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedContribution").value(425.00))
                .andExpect(jsonPath("$.amountPaid").value(190.00))
                .andExpect(jsonPath("$.remainingBalance").value(235.00));

        MockHttpSession adminSession = login("admin");
        mockMvc.perform(get("/api/admin/members").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].expectedContribution").value(425.00))
                .andExpect(jsonPath("$[0].amountPaid").value(190.00))
                .andExpect(jsonPath("$[0].remainingBalance").value(235.00));
        mockMvc.perform(get("/api/admin/reports").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedContribution").value(425.00))
                .andExpect(jsonPath("$.amountPaid").value(190.00))
                .andExpect(jsonPath("$.remainingBalance").value(235.00));
    }

    @Test
    void adminCanSearchSimilarNamesAndViewEachMembersContributionSummary() throws Exception {
        contributionRepository.deleteAll();
        paymentRepository.deleteAll();

        CooperativeRegistration adminOrganization = cooperativeRegistrationRepository.save(new CooperativeRegistration(
                "Search Cooperative", "search-cooperative", "Admin", "search@example.com", "admin", true, "search-token", LocalDateTime.now().plusHours(1)));
        CooperativeRegistration otherOrganization = cooperativeRegistrationRepository.save(new CooperativeRegistration(
                "Other Cooperative", "other-cooperative", "Other Admin", "other-search@example.com", "other-admin", true, "other-search-token", LocalDateTime.now().plusHours(1)));
        User admin = userRepository.findByUsername("admin").orElseThrow();
        admin.setCooperativeRegistration(adminOrganization);
        userRepository.save(admin);

        User firstUser = new User("alex-one", "alex-one@example.com", passwordEncoder.encode("secret123"), Role.ROLE_MEMBER);
        firstUser.setCooperativeRegistration(adminOrganization);
        userRepository.save(firstUser);
        Member firstMember = memberRepository.save(new Member("M-ALEX-1", "Alex Morgan", firstUser));

        User secondUser = new User("alex-two", "alex-two@example.com", passwordEncoder.encode("secret123"), Role.ROLE_MEMBER);
        secondUser.setCooperativeRegistration(adminOrganization);
        userRepository.save(secondUser);
        Member secondMember = memberRepository.save(new Member("M-ALEX-2", "Alexandra Morgan", secondUser));

        User otherUser = new User("alex-other", "alex-other@example.com", passwordEncoder.encode("secret123"), Role.ROLE_MEMBER);
        otherUser.setCooperativeRegistration(otherOrganization);
        userRepository.save(otherUser);
        memberRepository.save(new Member("M-ALEX-OTHER", "Alex Other", otherUser));

        Payment upcoming = paymentRepository.save(new Payment(firstMember, "SEARCH-UPCOMING", "Upcoming", new BigDecimal("120.00"), LocalDate.now().plusDays(5), PaymentStatus.UPCOMING));
        Payment overdue = paymentRepository.save(new Payment(firstMember, "SEARCH-OVERDUE", "Overdue", new BigDecimal("100.00"), LocalDate.now().minusDays(5), PaymentStatus.OVERDUE));
        Payment pending = paymentRepository.save(new Payment(firstMember, "SEARCH-PENDING", "Pending", new BigDecimal("75.00"), LocalDate.now().plusDays(1), PaymentStatus.PENDING));
        Payment paid = paymentRepository.save(new Payment(firstMember, "SEARCH-PAID", "Paid", new BigDecimal("50.00"), LocalDate.now().minusDays(10), PaymentStatus.PAID));
        Payment rejected = paymentRepository.save(new Payment(firstMember, "SEARCH-REJECTED", "Rejected", new BigDecimal("500.00"), LocalDate.now().minusDays(1), PaymentStatus.REJECTED));
        paymentRepository.save(new Payment(firstMember, "SEARCH-CANCELLED", "Cancelled", new BigDecimal("600.00"), LocalDate.now().plusDays(1), PaymentStatus.CANCELLED));

        Contribution firstPartial = new Contribution(firstMember, new BigDecimal("15.00"), LocalDateTime.now(), "Cash", "SEARCH-PARTIAL-1");
        firstPartial.setPayment(overdue);
        Contribution secondPartial = new Contribution(firstMember, new BigDecimal("25.00"), LocalDateTime.now(), "Cash", "SEARCH-PARTIAL-2");
        secondPartial.setPayment(overdue);
        Contribution paidMirror = new Contribution(firstMember, new BigDecimal("50.00"), LocalDateTime.now(), "System", "SEARCH-PAID-MIRROR");
        paidMirror.setPayment(paid);
        Contribution pendingTransaction = new Contribution(firstMember, new BigDecimal("75.00"), LocalDateTime.now(), "Cash", "SEARCH-PENDING-TX");
        pendingTransaction.setPayment(pending);
        Contribution standalonePartial = new Contribution(firstMember, new BigDecimal("10.00"), LocalDateTime.now(), "Cash", "SEARCH-STANDALONE");
        contributionRepository.saveAll(java.util.List.of(firstPartial, secondPartial, paidMirror, pendingTransaction, standalonePartial));

        MockHttpSession adminSession = login("admin");
        mockMvc.perform(get("/api/admin/members/search").param("name", "alex").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/admin/members/search").param("name", "no matching person").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/admin/members/{id}", firstMember.getId()).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("Search Cooperative"))
                .andExpect(jsonPath("$.upcomingContributions").value(120.00))
                .andExpect(jsonPath("$.overdueContributions").value(100.00))
                .andExpect(jsonPath("$.pendingContributions").value(75.00))
                .andExpect(jsonPath("$.expectedContribution").value(295.00))
                .andExpect(jsonPath("$.amountPaid").value(100.00))
                .andExpect(jsonPath("$.remainingBalance").value(195.00))
                .andExpect(jsonPath("$.hasContributionRecords").value(true));
        mockMvc.perform(get("/api/admin/members/{id}", secondMember.getId()).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedContribution").value(0))
                .andExpect(jsonPath("$.amountPaid").value(0))
                .andExpect(jsonPath("$.hasContributionRecords").value(false));
    }

    @Test
    void noPaymentAndAdvanceOverpaymentProduceNonNegativeBalances() throws Exception {
        contributionRepository.deleteAll();
        paymentRepository.deleteAll();

        User noPaymentUser = new User("empty-member", "empty@example.com", passwordEncoder.encode("secret123"), Role.ROLE_MEMBER);
        userRepository.save(noPaymentUser);
        memberRepository.save(new Member("M-EMPTY", "Empty Member", noPaymentUser));

        MockHttpSession emptySession = login("empty-member");
        mockMvc.perform(get("/api/member/dashboard").session(emptySession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedContribution").value(0))
                .andExpect(jsonPath("$.amountPaid").value(0))
                .andExpect(jsonPath("$.remainingBalance").value(0));

        User fullPaymentUser = new User("full-member", "full@example.com", passwordEncoder.encode("secret123"), Role.ROLE_MEMBER);
        userRepository.save(fullPaymentUser);
        Member fullPaymentMember = memberRepository.save(new Member("M-FULL", "Full Member", fullPaymentUser));
        paymentRepository.save(new Payment(fullPaymentMember, "CALC-FULL", "Paid contribution", new BigDecimal("100.00"), LocalDate.now().minusDays(1), PaymentStatus.PAID));

        MockHttpSession fullPaymentSession = login("full-member");
        mockMvc.perform(get("/api/member/dashboard").session(fullPaymentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedContribution").value(0))
                .andExpect(jsonPath("$.amountPaid").value(100.00))
                .andExpect(jsonPath("$.remainingBalance").value(0));

        Member member = memberRepository.findByMemberNumber("M-1001").orElseThrow();
        Payment duePayment = paymentRepository.save(new Payment(member, "CALC-DUE", "Due contribution", new BigDecimal("100.00"), LocalDate.now().minusDays(1), PaymentStatus.SCHEDULED));
        paymentRepository.save(new Payment(member, "CALC-ADVANCE", "Advance payment", new BigDecimal("250.00"), LocalDate.now().plusDays(10), PaymentStatus.PAID));

        MockHttpSession memberSession = login("member");
        mockMvc.perform(get("/api/member/dashboard").session(memberSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedContribution").value(100.00))
                .andExpect(jsonPath("$.amountPaid").value(250.00))
                .andExpect(jsonPath("$.remainingBalance").value(0));

        MockHttpSession adminSession = login("admin");
        mockMvc.perform(put("/api/admin/payments/{id}", duePayment.getId())
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceNumber\":\"CALC-DUE\",\"description\":\"Updated due contribution\",\"amount\":300.00,\"dueDate\":\"" + LocalDate.now().minusDays(1) + "\",\"status\":\"SCHEDULED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/member/dashboard").session(memberSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedContribution").value(300.00))
                .andExpect(jsonPath("$.amountPaid").value(250.00))
                .andExpect(jsonPath("$.remainingBalance").value(50.00));
    }

    @Test
    void confirmingPaymentTwiceDoesNotCreateDuplicateContributionRows() throws Exception {
        contributionRepository.deleteAll();
        Payment payment = paymentRepository.findAll().get(0);
        MockHttpSession adminSession = login("admin");

        mockMvc.perform(post("/api/admin/payments/{id}/confirm", payment.getId()).session(adminSession))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/payments/{id}/confirm", payment.getId()).session(adminSession))
                .andExpect(status().isOk());

        assertEquals(1, contributionRepository.findByMemberOrderByPaymentDateDesc(payment.getMember()).stream()
                .filter(contribution -> payment.equals(contribution.getPayment())).count());
    }

    @Test
    void adminCalculationsAreLimitedToTheAdminsOrganization() throws Exception {
        contributionRepository.deleteAll();
        paymentRepository.deleteAll();

        CooperativeRegistration firstOrganization = cooperativeRegistrationRepository.save(new CooperativeRegistration(
                "First Cooperative", "first-cooperative", "Admin", "first@example.com", "admin", true, "first-token", LocalDateTime.now().plusHours(1)));
        CooperativeRegistration secondOrganization = cooperativeRegistrationRepository.save(new CooperativeRegistration(
                "Second Cooperative", "second-cooperative", "Other Admin", "other@example.com", "other-admin", true, "second-token", LocalDateTime.now().plusHours(1)));

        User admin = userRepository.findByUsername("admin").orElseThrow();
        admin.setCooperativeRegistration(firstOrganization);
        userRepository.save(admin);
        Member firstMember = memberRepository.findByMemberNumber("M-1001").orElseThrow();
        firstMember.getUser().setCooperativeRegistration(firstOrganization);
        userRepository.save(firstMember.getUser());

        User otherUser = new User("other-member", "other-member@example.com", passwordEncoder.encode("secret123"), Role.ROLE_MEMBER);
        otherUser.setCooperativeRegistration(secondOrganization);
        userRepository.save(otherUser);
        Member otherMember = memberRepository.save(new Member("M-OTHER", "Other Member", otherUser));

        Payment firstDue = paymentRepository.save(new Payment(firstMember, "ORG-FIRST", "First org due", new BigDecimal("100.00"), LocalDate.now().minusDays(1), PaymentStatus.SCHEDULED));
        Payment otherOrganizationPayment = paymentRepository.save(new Payment(otherMember, "ORG-OTHER", "Other org payment", new BigDecimal("500.00"), LocalDate.now().minusDays(1), PaymentStatus.SCHEDULED));
        Contribution firstPartial = new Contribution(firstMember, new BigDecimal("20.00"), LocalDateTime.now(), "Cash", "ORG-FIRST-PARTIAL");
        firstPartial.setPayment(firstDue);
        Contribution otherMemberTransaction = new Contribution(firstMember, new BigDecimal("500.00"), LocalDateTime.now(), "Cash", "ORG-OTHER-PARTIAL");
        otherMemberTransaction.setPayment(otherOrganizationPayment);
        contributionRepository.saveAll(java.util.List.of(firstPartial, otherMemberTransaction));

        MockHttpSession adminSession = login("admin");
        mockMvc.perform(get("/api/admin/members").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/admin/payments").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/admin/reports").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedContribution").value(100.00))
                .andExpect(jsonPath("$.amountPaid").value(20.00))
                .andExpect(jsonPath("$.remainingBalance").value(80.00));
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
                .andExpect(jsonPath("$.organizationName").value("No Organization Assigned"))
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

    @Test
    void adminCanIssueReceiptAndSendItToMemberNotifications() throws Exception {
        Payment payment = paymentRepository.findAll().get(0);
        MockHttpSession adminSession = login("admin");

        mockMvc.perform(post("/api/admin/payments/{id}/confirm", payment.getId()).session(adminSession))
                .andExpect(status().isOk());

        String receipt = mockMvc.perform(post("/api/admin/payments/{id}/receipt", payment.getId()).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(receipt.contains("PAY-1001"));
        assertTrue(receipt.contains("Member User"));

        Member member = memberRepository.findAll().get(0);
        Notification receiptNotification = notificationRepository.findByMemberOrderByCreatedDateDesc(member).stream()
                .filter(notification -> "Payment receipt issued".equals(notification.getTitle()))
                .findFirst()
                .orElseThrow();
        assertEquals(receipt, receiptNotification.getMessage());
    }

    @Test
    void adminCannotIssueReceiptForUnpaidPayment() throws Exception {
        Payment payment = paymentRepository.findAll().get(0);
        MockHttpSession adminSession = login("admin");

        mockMvc.perform(post("/api/admin/payments/{id}/receipt", payment.getId()).session(adminSession))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminAndMemberCanUploadAndRetrieveTheirOwnProfilePictures() throws Exception {
        byte[] adminImage = createPng(0xFF0000);
        byte[] memberImage = createPng(0x0000FF);
        MockHttpSession adminSession = login("admin");
        MockHttpSession memberSession = login("member");

        mockMvc.perform(multipart("/api/profile/photo")
                        .file(new MockMultipartFile("file", "admin.png", "image/png", adminImage))
                        .session(adminSession))
                .andExpect(status().isNoContent());
        mockMvc.perform(multipart("/api/profile/photo")
                        .file(new MockMultipartFile("file", "member.png", "image/png", memberImage))
                        .session(memberSession))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/profile/photo").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(adminImage));
        mockMvc.perform(get("/api/profile/photo").session(memberSession))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(memberImage));
    }

    @Test
    void profilePictureUploadRejectsNonImageFiles() throws Exception {
        MockHttpSession memberSession = login("member");
        MockMultipartFile invalidImage = new MockMultipartFile("file", "avatar.png", "image/png", "not an image".getBytes());

        mockMvc.perform(multipart("/api/profile/photo").file(invalidImage).session(memberSession))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession login(String username) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    }

    private byte[] createPng(int rgb) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, rgb);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
