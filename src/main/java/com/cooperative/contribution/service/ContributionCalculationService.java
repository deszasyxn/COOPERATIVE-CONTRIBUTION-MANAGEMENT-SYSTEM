package com.cooperative.contribution.service;

import com.cooperative.contribution.dto.ContributionDto;
import com.cooperative.contribution.dto.ContributionSummaryDto;
import com.cooperative.contribution.entity.Contribution;
import com.cooperative.contribution.entity.Payment;
import com.cooperative.contribution.enumeration.PaymentStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class ContributionCalculationService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public ContributionSummaryDto calculate(List<Payment> payments, List<Contribution> contributions, LocalDate asOfDate) {
        List<Payment> paymentRecords = payments == null
                ? List.of()
                : payments.stream().filter(Objects::nonNull).toList();
        List<Contribution> contributionRecords = contributions == null
                ? List.of()
                : contributions.stream().filter(Objects::nonNull).toList();
        LocalDate effectiveAsOfDate = asOfDate == null ? LocalDate.now() : asOfDate;

        BigDecimal expectedContribution = ZERO;
        BigDecimal upcomingContributions = ZERO;
        BigDecimal overdueContributions = ZERO;
        BigDecimal pendingContributions = ZERO;

        for (Payment payment : paymentRecords) {
            PaymentStatus status = payment.getStatus();
            LocalDate dueDate = payment.getDueDate();
            BigDecimal amount = normalizedAmount(payment.getAmount());

            if (dueDate != null && status != PaymentStatus.REJECTED && status != PaymentStatus.CANCELLED
                    && !dueDate.isAfter(effectiveAsOfDate)) {
                expectedContribution = expectedContribution.add(amount);
            }

            if (status == PaymentStatus.SCHEDULED || status == PaymentStatus.UPCOMING) {
                pendingContributions = pendingContributions.add(amount);
            }

            if (dueDate != null && isOutstanding(status)) {
                if (dueDate.isAfter(effectiveAsOfDate)) {
                    upcomingContributions = upcomingContributions.add(amount);
                } else if (dueDate.isBefore(effectiveAsOfDate)) {
                    overdueContributions = overdueContributions.add(amount);
                }
            }
        }

        BigDecimal amountPaid = contributionRecords.stream()
                .map(Contribution::getAmount)
                .map(this::normalizedAmount)
                .reduce(ZERO, BigDecimal::add);

        ContributionSummaryDto summary = new ContributionSummaryDto();
        summary.setExpectedContribution(expectedContribution);
        summary.setUpcomingContributions(upcomingContributions);
        summary.setOverdueContributions(overdueContributions);
        summary.setPendingContributions(pendingContributions);
        summary.setAmountPaid(amountPaid);
        summary.setRemainingBalance(expectedContribution.subtract(amountPaid).max(ZERO));
        summary.setHasContributionRecords(!contributionRecords.isEmpty());
        return summary;
    }

    public List<ContributionDto> getPaidTransactions(List<Payment> payments, List<Contribution> contributions) {
        if (contributions == null) {
            return List.of();
        }
        return contributions.stream()
                .filter(Objects::nonNull)
                .map(this::toContributionDto)
                .sorted(Comparator.comparing(ContributionDto::getPaymentDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private boolean isOutstanding(PaymentStatus status) {
        return status != PaymentStatus.PAID
                && status != PaymentStatus.REJECTED
                && status != PaymentStatus.CANCELLED;
    }

    private BigDecimal normalizedAmount(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private ContributionDto toContributionDto(Contribution contribution) {
        return new ContributionDto(
                contribution.getId(),
                normalizedAmount(contribution.getAmount()),
                contribution.getPaymentDate(),
                contribution.getPaymentMethod(),
                contribution.getReferenceNumber()
        );
    }
}