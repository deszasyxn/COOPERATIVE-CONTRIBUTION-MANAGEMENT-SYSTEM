package com.cooperative.contribution.repository;

import com.cooperative.contribution.entity.Contribution;
import com.cooperative.contribution.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    List<Contribution> findByMemberOrderByPaymentDateDesc(Member member);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c WHERE c.member = :member")
    BigDecimal sumAmountByMember(@Param("member") Member member);

    @Query("SELECT COUNT(c) FROM Contribution c WHERE c.member = :member")
    long countByMember(@Param("member") Member member);
}
