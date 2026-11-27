package com.cooperative.contribution.repository;

import com.cooperative.contribution.entity.Contribution;
import com.cooperative.contribution.entity.Member;
import com.cooperative.contribution.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    List<Contribution> findByMemberOrderByPaymentDateDesc(Member member);

    List<Contribution> findByPayment(Payment payment);
}
