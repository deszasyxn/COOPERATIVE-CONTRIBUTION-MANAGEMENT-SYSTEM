package com.cooperative.contribution.repository;

import com.cooperative.contribution.entity.Member;
import com.cooperative.contribution.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByMemberOrderByCreatedDateDesc(Member member);

    List<Notification> findByMemberAndReadFalseOrderByCreatedDateDesc(Member member);

    long countByMemberAndReadFalse(Member member);
}
