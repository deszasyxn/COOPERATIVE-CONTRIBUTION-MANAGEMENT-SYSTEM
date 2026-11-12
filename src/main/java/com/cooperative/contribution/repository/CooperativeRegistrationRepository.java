package com.cooperative.contribution.repository;

import com.cooperative.contribution.entity.CooperativeRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CooperativeRegistrationRepository extends JpaRepository<CooperativeRegistration, Long> {

    boolean existsByNormalizedName(String normalizedName);

    Optional<CooperativeRegistration> findByNormalizedName(String normalizedName);

    Optional<CooperativeRegistration> findBySetupTokenHash(String setupTokenHash);
}
