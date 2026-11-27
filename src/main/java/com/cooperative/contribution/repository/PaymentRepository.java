package com.cooperative.contribution.repository;

import com.cooperative.contribution.entity.Member;
import com.cooperative.contribution.entity.Payment;
import com.cooperative.contribution.enumeration.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByMemberOrderByDueDateAsc(Member member);

    List<Payment> findByMemberAndStatusOrderByDueDateAsc(Member member, PaymentStatus status);

    Optional<Payment> findFirstByMemberAndStatusNotOrderByDueDateAsc(Member member, PaymentStatus status);

    long countByMemberAndStatus(Member member, PaymentStatus status);

    List<Payment> findAllByOrderByDueDateDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :paymentId")
    Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);
}
