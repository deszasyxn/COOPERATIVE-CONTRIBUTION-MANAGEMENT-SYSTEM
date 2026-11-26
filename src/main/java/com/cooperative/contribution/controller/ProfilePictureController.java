package com.cooperative.contribution.controller;

import com.cooperative.contribution.entity.ProfilePicture;
import com.cooperative.contribution.service.ProfilePictureService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile/photo")
@PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
public class ProfilePictureController {

    private final ProfilePictureService profilePictureService;

    public ProfilePictureController(ProfilePictureService profilePictureService) {
        this.profilePictureService = profilePictureService;
    }

    @GetMapping
    public ResponseEntity<byte[]> getProfilePicture() {
        ProfilePicture picture = profilePictureService.getCurrentProfilePicture();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(picture.getContentType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(picture.getData());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        profilePictureService.uploadProfilePicture(file);
        return ResponseEntity.noContent().build();
    }
}