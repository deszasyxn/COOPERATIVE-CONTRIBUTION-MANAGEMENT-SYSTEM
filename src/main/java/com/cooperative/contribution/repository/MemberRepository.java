package com.cooperative.contribution.repository;

import com.cooperative.contribution.entity.Member;
import com.cooperative.contribution.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUser(User user);

    Optional<Member> findByUserId(Long userId);

    Optional<Member> findByMemberNumber(String memberNumber);

    List<Member> findAllByOrderByDateJoinedDesc();
}
