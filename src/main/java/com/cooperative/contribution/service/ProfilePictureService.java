package com.cooperative.contribution.service;

import com.cooperative.contribution.entity.ProfilePicture;
import com.cooperative.contribution.entity.User;
import com.cooperative.contribution.exception.ApiException;
import com.cooperative.contribution.repository.ProfilePictureRepository;
import com.cooperative.contribution.repository.UserRepository;
import com.cooperative.contribution.security.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Locale;

@Service
@Transactional
public class ProfilePictureService {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 2048;
    private static final String PNG_CONTENT_TYPE = "image/png";
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";

    private final ProfilePictureRepository profilePictureRepository;
    private final UserRepository userRepository;

    public ProfilePictureService(ProfilePictureRepository profilePictureRepository, UserRepository userRepository) {
        this.profilePictureRepository = profilePictureRepository;
        this.userRepository = userRepository;
    }

    public ProfilePicture getCurrentProfilePicture() {
        User currentUser = SecurityUtil.getCurrentUser();
        return profilePictureRepository.findByUser_Id(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile picture not found."));
    }

    public void uploadProfilePicture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a PNG or JPEG profile picture to upload.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Profile pictures must not exceed 2 MB.");
        }

        byte[] imageData;
        String contentType;
        try {
            imageData = file.getBytes();
            contentType = validateImage(imageData, file.getContentType());
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload a valid PNG or JPEG image.");
        }

        User currentUser = SecurityUtil.getCurrentUser();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found."));
        ProfilePicture picture = profilePictureRepository.findByUser_Id(user.getId())
                .orElseGet(() -> new ProfilePicture(user, imageData, contentType));
        picture.setData(imageData);
        picture.setContentType(contentType);
        profilePictureRepository.save(picture);
    }

    private String validateImage(byte[] imageData, String submittedContentType) throws IOException {
        if (!PNG_CONTENT_TYPE.equalsIgnoreCase(submittedContentType)
                && !JPEG_CONTENT_TYPE.equalsIgnoreCase(submittedContentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PNG and JPEG profile pictures are supported.");
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            if (input == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Upload a valid PNG or JPEG image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Only PNG and JPEG profile pictures are supported.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String contentType = switch (format) {
                    case "png" -> PNG_CONTENT_TYPE;
                    case "jpeg", "jpg" -> JPEG_CONTENT_TYPE;
                    default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Only PNG and JPEG profile pictures are supported.");
                };
                if (!contentType.equalsIgnoreCase(submittedContentType)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "The uploaded image format does not match its content type.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Profile pictures must be no larger than 2048 by 2048 pixels.");
                }
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
                if (image == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Upload a valid PNG or JPEG image.");
                }
                image.flush();
                return contentType;
            } finally {
                reader.dispose();
            }
        }
    }
}