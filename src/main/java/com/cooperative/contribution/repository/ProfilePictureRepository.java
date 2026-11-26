package com.cooperative.contribution.repository;

import com.cooperative.contribution.entity.ProfilePicture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfilePictureRepository extends JpaRepository<ProfilePicture, Long> {

    Optional<ProfilePicture> findByUser_Id(Long userId);
}