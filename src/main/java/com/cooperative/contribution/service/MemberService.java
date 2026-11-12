package com.cooperative.contribution.service;

import com.cooperative.contribution.dto.*;
import com.cooperative.contribution.entity.*;
import com.cooperative.contribution.enumeration.*;
import com.cooperative.contribution.exception.ApiException;
import com.cooperative.contribution.repository.*;
import com.cooperative.contribution.security.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final ContributionRepository contributionRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public MemberService(MemberRepository memberRepository, PaymentRepository paymentRepository,
                         ContributionRepository contributionRepository, NotificationRepository notificationRepository,
                         UserRepository userRepository) {
        this.memberRepository = memberRepository;
        this.paymentRepository = paymentRepository;
        this.contributionRepository = contributionRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public Member getCurrentMember() {
        User currentUser = SecurityUtil.getCurrentUser();
        return memberRepository.findByUser(currentUser)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member profile not found."));
    }

    public MemberDashboardDto getDashboard() {
        Member member = getCurrentMember();
        List<Payment> payments = paymentRepository.findByMemberOrderByDueDateAsc(member);
        BigDecimal totalContributions = contributionRepository.sumAmountByMember(member);
        if (totalContributions == null) {
            totalContributions = BigDecimal.ZERO;
        }

        Payment nextPayment = payments.stream()
                .filter(payment -> payment.getStatus() != PaymentStatus.PAID)
                .sorted(Comparator.comparing(Payment::getDueDate))
                .findFirst()
                .orElse(null);

        BigDecimal nextAmount = nextPayment != null ? nextPayment.getAmount() : BigDecimal.ZERO;
        LocalDate nextDueDate = nextPayment != null ? nextPayment.getDueDate() : null;

        long completedPayments = paymentRepository.countByMemberAndStatus(member, PaymentStatus.PAID);
        long scheduledPayments = payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SCHEDULED || payment.getStatus() == PaymentStatus.UPCOMING)
                .count();

        MemberDashboardDto dashboard = new MemberDashboardDto();
        dashboard.setMemberName(member.getName());
        dashboard.setMemberNumber(member.getMemberNumber());
        dashboard.setTotalContributions(totalContributions);
        dashboard.setNextPaymentAmount(nextAmount);
        dashboard.setNextPaymentDueDate(nextDueDate);
        dashboard.setCompletedPayments(completedPayments);
        dashboard.setScheduledPayments(scheduledPayments);
        dashboard.setAccountStanding(member.getMembershipStatus() == com.cooperative.contribution.enumeration.MembershipStatus.ACTIVE ? "Good standing" : "Review required");
        dashboard.setUpcomingPayments(payments.stream().limit(3).map(this::toPaymentDto).collect(Collectors.toList()));
        dashboard.setRecentActivities(contributionRepository.findByMemberOrderByPaymentDateDesc(member).stream().limit(5).map(this::toContributionDto).collect(Collectors.toList()));
        dashboard.setMonthlyContributionStats(buildMonthlyContributionStats(member));
        return dashboard;
    }

    public MemberProfileDto getProfile() {
        Member member = getCurrentMember();
        return new MemberProfileDto(
                member.getId(),
                member.getMemberNumber(),
                member.getName(),
                member.getUser().getEmail(),
                member.getPhone(),
                member.getAddress(),
                member.getMembershipStatus(),
                member.getDateJoined()
        );
    }

    public MemberProfileDto updateProfile(ProfileUpdateRequest request) {
        Member member = getCurrentMember();
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
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String normalizedEmail = request.getEmail().trim();
            userRepository.findByEmail(normalizedEmail)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new ApiException(HttpStatus.CONFLICT, "Email is already in use.");
                    });
            user.setEmail(normalizedEmail);
        }

        memberRepository.save(member);
        userRepository.save(user);

        return getProfile();
    }

    public List<PaymentDto> getPayments() {
        return paymentRepository.findByMemberOrderByDueDateAsc(getCurrentMember())
                .stream().map(this::toPaymentDto).collect(Collectors.toList());
    }

    public List<PaymentDto> getUpcomingPayments() {
        return paymentRepository.findByMemberOrderByDueDateAsc(getCurrentMember()).stream()
                .filter(payment -> payment.getStatus() != PaymentStatus.PAID)
                .map(this::toPaymentDto)
                .collect(Collectors.toList());
    }

    public PaymentDto getPaymentById(Long paymentId) {
        Member member = getCurrentMember();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found."));
        if (!payment.getMember().getId().equals(member.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot access this payment record.");
        }
        return toPaymentDto(payment);
    }

    public List<ContributionDto> getContributions() {
        return contributionRepository.findByMemberOrderByPaymentDateDesc(getCurrentMember()).stream()
                .map(this::toContributionDto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getContributionSummary() {
        Member member = getCurrentMember();
        BigDecimal total = contributionRepository.sumAmountByMember(member);
        if (total == null) {
            total = BigDecimal.ZERO;
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalContributions", total);
        summary.put("count", contributionRepository.countByMember(member));
        summary.put("monthlyStatistics", buildMonthlyContributionStats(member));
        return summary;
    }

    public List<PaymentDto> getPaymentHistory() {
        return paymentRepository.findByMemberOrderByDueDateAsc(getCurrentMember()).stream()
                .map(this::toPaymentDto)
                .collect(Collectors.toList());
    }

    public List<NotificationDto> getNotifications() {
        return notificationRepository.findByMemberOrderByCreatedDateDesc(getCurrentMember()).stream()
                .map(this::toNotificationDto)
                .collect(Collectors.toList());
    }

    public List<NotificationDto> getUnreadNotifications() {
        return notificationRepository.findByMemberAndReadFalseOrderByCreatedDateDesc(getCurrentMember()).stream()
                .map(this::toNotificationDto)
                .collect(Collectors.toList());
    }

    public NotificationDto markNotificationRead(Long notificationId) {
        Member member = getCurrentMember();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found."));
        if (!notification.getMember().getId().equals(member.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot access this notification.");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
        return toNotificationDto(notification);
    }

    public void markAllNotificationsRead() {
        Member member = getCurrentMember();
        List<Notification> unread = notificationRepository.findByMemberAndReadFalseOrderByCreatedDateDesc(member);
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private Map<String, BigDecimal> buildMonthlyContributionStats(Member member) {
        Map<String, BigDecimal> stats = new LinkedHashMap<>();
        List<String> months = List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");
        for (String month : months) {
            stats.put(month, BigDecimal.ZERO);
        }

        List<Contribution> contributions = contributionRepository.findByMemberOrderByPaymentDateDesc(member);
        for (Contribution contribution : contributions) {
            YearMonth yearMonth = YearMonth.from(contribution.getPaymentDate());
            String key = yearMonth.getMonth().name().substring(0, 3).toUpperCase();
            if (stats.containsKey(key)) {
                stats.put(key, stats.get(key).add(contribution.getAmount()));
            }
        }
        return stats;
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

    private ContributionDto toContributionDto(Contribution contribution) {
        return new ContributionDto(
                contribution.getId(),
                contribution.getAmount(),
                contribution.getPaymentDate(),
                contribution.getPaymentMethod(),
                contribution.getReferenceNumber()
        );
    }

    private NotificationDto toNotificationDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getNotificationType(),
                notification.isRead(),
                notification.getCreatedDate()
        );
    }
}
