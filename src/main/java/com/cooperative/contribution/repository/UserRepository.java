package com.cooperative.contribution.repository;

import com.cooperative.contribution.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username = :login OR LOWER(u.email) = LOWER(:login)")
    Optional<User> findByUsernameOrEmail(@Param("login") String login);
}
