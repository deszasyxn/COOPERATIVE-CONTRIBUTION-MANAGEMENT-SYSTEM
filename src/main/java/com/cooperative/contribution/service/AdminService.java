package com.cooperative.contribution.service;

import com.cooperative.contribution.dto.*;
import com.cooperative.contribution.entity.*;
import com.cooperative.contribution.enumeration.*;
import com.cooperative.contribution.exception.ApiException;
import com.cooperative.contribution.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ContributionRepository contributionRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(MemberRepository memberRepository, UserRepository userRepository,
                        PaymentRepository paymentRepository, ContributionRepository contributionRepository,
                        NotificationRepository notificationRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.contributionRepository = contributionRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdminMemberDto> getMembers() {
        return memberRepository.findAllByOrderByDateJoinedDesc().stream()
                .map(this::toAdminMemberDto)
                .collect(Collectors.toList());
    }

    public AdminMemberDto getMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found."));
        return toAdminMemberDto(member);
    }

    public AdminMemberDto createMember(AdminMemberRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username is required for new members.");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required for new members.");
        }

        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        userRepository.findByUsername(username)
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Username already exists.");
                });
        userRepository.findByEmail(email)
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Email already exists.");
                });

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_MEMBER);
        user.setEnabled(true);
        user = userRepository.save(user);

        Member member = new Member(generateMemberNumber(), request.getName(), user);
        member.setPhone(request.getPhone());
        member.setAddress(request.getAddress());
        member.setMembershipStatus(request.getMembershipStatus() != null ? request.getMembershipStatus() : MembershipStatus.ACTIVE);
        member.setUser(user);
        memberRepository.save(member);

        Notification notification = new Notification(member, "Welcome to MemberHub", "Your member account has been created successfully.", NotificationType.INFORMATION);
        notificationRepository.save(notification);

        return toAdminMemberDto(member);
    }

    public AdminMemberDto updateMember(Long id, AdminMemberRequest request) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found."));
        User user = member.getUser();

        if (request.getName() != null && !request.getName().isBlank()) {
            member.setName(request.getName());
        }
        if (request.getPhone() != null) {
            member.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            member.setAddress(request.getAddress());
        }
        if (request.getMembershipStatus() != null) {
            member.setMembershipStatus(request.getMembershipStatus());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
            userRepository.findByEmail(email)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new ApiException(HttpStatus.CONFLICT, "Email already exists.");
                    });
            user.setEmail(email);
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
            userRepository.findByUsername(username)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new ApiException(HttpStatus.CONFLICT, "Username already exists.");
                    });
            user.setUsername(username);
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        memberRepository.save(member);
        userRepository.save(user);
        return toAdminMemberDto(member);
    }

    public AdminMemberDto updateMemberStatus(Long id, StatusUpdateRequest request) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found."));
        if (request.getMembershipStatus() != null) {
            member.setMembershipStatus(request.getMembershipStatus());
        }
        memberRepository.save(member);
        return toAdminMemberDto(member);
    }

    public PaymentDto createPayment(AdminPaymentRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found."));

        Payment payment = new Payment(
                member,
                request.getReferenceNumber(),
                request.getDescription(),
                request.getAmount(),
                request.getDueDate(),
                request.getStatus() != null ? request.getStatus() : PaymentStatus.SCHEDULED
        );

        Payment saved = paymentRepository.save(payment);
        Notification notification = new Notification(member, "Payment scheduled", "A new payment entry has been added for " + request.getDescription() + ".", NotificationType.PAYMENT);
        notificationRepository.save(notification);
        return toPaymentDto(saved);
    }

    public PaymentDto updatePayment(Long paymentId, PaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found."));

        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setDescription(request.getDescription());
        payment.setAmount(request.getAmount());
        payment.setDueDate(request.getDueDate());
        if (request.getStatus() != null) {
            payment.setStatus(request.getStatus());
        }

        Payment saved = paymentRepository.save(payment);
        return toPaymentDto(saved);
    }

    public PaymentDto confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found."));

        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);

        Contribution contribution = new Contribution(
                payment.getMember(),
                payment.getAmount(),
                LocalDateTime.now(),
                "System",
                "CONTRIB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
        contribution.setPayment(payment);
        contributionRepository.save(contribution);

        Notification notification = new Notification(payment.getMember(), "Payment confirmed", "Payment " + payment.getReferenceNumber() + " has been confirmed.", NotificationType.PAYMENT);
        notificationRepository.save(notification);
        return toPaymentDto(payment);
    }

    public String issueReceipt(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found."));
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A receipt can only be issued for a confirmed payment.");
        }

        Member member = payment.getMember();
        String receipt = String.join("\n",
                "COOPERATIVE CONTRIBUTION RECEIPT",
                "Receipt number: " + payment.getReferenceNumber(),
                "Member: " + member.getName(),
                "Member number: " + member.getMemberNumber(),
                "Description: " + payment.getDescription(),
                "Amount received: PHP " + payment.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                "Payment status: " + payment.getStatus(),
                "Receipt issued: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "",
                "This receipt confirms that the payment above was recorded by the cooperative.",
                "");

        notificationRepository.save(new Notification(member, "Payment receipt issued", receipt, NotificationType.PAYMENT));
        return receipt;
    }

    public List<PaymentDto> getAllPayments() {
        return paymentRepository.findAllByOrderByDueDateDesc().stream()
                .map(this::toPaymentDto)
                .collect(Collectors.toList());
    }

    public AdminReportDto getReport() {
        long totalMembers = memberRepository.count();
        long upcomingPaymentCount = paymentRepository.findAll().stream()
                .filter(payment -> payment.getStatus() != PaymentStatus.PAID)
                .count();
        BigDecimal totalContributions = contributionRepository.findAll().stream()
                .map(Contribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCollected = paymentRepository.findAll().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdminReportDto(totalMembers, upcomingPaymentCount, totalContributions, totalCollected);
    }

    private String generateMemberNumber() {
        return "M-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private AdminMemberDto toAdminMemberDto(Member member) {
        return new AdminMemberDto(
                member.getId(),
                member.getMemberNumber(),
                member.getName(),
                member.getUser().getEmail(),
                member.getPhone(),
                member.getAddress(),
                member.getMembershipStatus()
        );
    }

    private PaymentDto toPaymentDto(Payment payment) {
        return new PaymentDto(
                payment.getId(),
                payment.getReferenceNumber(),
                payment.getDescription(),
                payment.getAmount(),
                payment.getDueDate(),
                payment.getStatus()
        );
    }
}
