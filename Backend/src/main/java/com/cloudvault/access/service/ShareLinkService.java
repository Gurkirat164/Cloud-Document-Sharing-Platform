package com.cloudvault.access.service;

import com.cloudvault.access.dto.CreateShareLinkRequest;
import com.cloudvault.access.dto.ShareLinkResponse;
import com.cloudvault.access.repository.ShareLinkRepository;
import com.cloudvault.common.exception.AccessDeniedException;
import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.common.util.UuidUtils;
import com.cloudvault.domain.File;
import com.cloudvault.domain.ShareLink;
import com.cloudvault.domain.User;
import com.cloudvault.file.repository.FileRepository;
import com.cloudvault.file.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final FileRepository fileRepository;
    private final S3Service s3Service;
    private final PasswordEncoder passwordEncoder;

    public ShareLinkResponse createShareLink(String fileUuid, CreateShareLinkRequest request, User currentUser) {
        File file = fileRepository.findByUuid(fileUuid)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the file owner can create share links");
        }

        String token = UuidUtils.newUuid().replace("-", "");

        String passwordHash = null;
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            passwordHash = passwordEncoder.encode(request.getPassword());
        }

        Instant expiresAt = null;
        if (request.getExpirationHours() != null) {
            expiresAt = Instant.now().plus(request.getExpirationHours(), ChronoUnit.HOURS);
        }

        ShareLink shareLink = ShareLink.builder()
                .token(token)
                .file(file)
                .createdBy(currentUser)
                .permission(request.getPermission())
                .passwordHash(passwordHash)
                .maxUses(request.getMaxUses())
                .expiresAt(expiresAt)
                .isActive(true)
                .build();

        ShareLink savedLink = shareLinkRepository.save(shareLink);
        log.info("Share link created for file {} by {}", fileUuid, currentUser.getEmail());
        
        return ShareLinkResponse.from(savedLink);
    }

    public String accessShareLink(String token, String providedPassword) {
        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found"));

        if (!shareLink.isValid()) {
            throw new AccessDeniedException("Share link is expired, inactive, or has reached its max uses");
        }

        if (shareLink.getPasswordHash() != null) {
            if (providedPassword == null) {
                throw new AccessDeniedException("Password is required to access this share link");
            }
            if (!passwordEncoder.matches(providedPassword, shareLink.getPasswordHash())) {
                throw new AccessDeniedException("Incorrect password");
            }
        }

        shareLink.setUseCount(shareLink.getUseCount() + 1);
        shareLinkRepository.save(shareLink);

        File file = shareLink.getFile();
        
        long durationMinutes = 5;
        
        log.info("Share link accessed for file {}", file.getUuid());
        return s3Service.generatePresignedGetUrl(file.getS3Key(), file.getOriginalName(), durationMinutes);
    }
}
