package com.cloudvault.access;

import com.cloudvault.access.dto.CreateShareLinkRequest;
import com.cloudvault.access.repository.ShareLinkRepository;
import com.cloudvault.access.service.ShareLinkService;
import com.cloudvault.common.exception.AccessDeniedException;
import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.domain.File;
import com.cloudvault.domain.ShareLink;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.SharePermission;
import com.cloudvault.file.repository.FileRepository;
import com.cloudvault.file.service.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShareLinkServiceTest {

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ShareLinkService shareLinkService;

    @Test
    void createShareLink_success() {
        User user = User.builder().id(1L).email("user@test.com").build();
        File file = File.builder().id(10L).uuid("file-uuid").owner(user).build();
        
        when(fileRepository.findByUuid("file-uuid")).thenReturn(Optional.of(file));
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
        
        ShareLink savedLink = ShareLink.builder().token("token123").file(file).permission(SharePermission.VIEW).isActive(true).build();
        when(shareLinkRepository.save(any(ShareLink.class))).thenReturn(savedLink);
        
        CreateShareLinkRequest request = new CreateShareLinkRequest();
        request.setPassword("secret");
        request.setPermission(SharePermission.VIEW);
        request.setMaxUses(5);
        request.setExpirationHours(24);
        
        var response = shareLinkService.createShareLink("file-uuid", request, user);
        
        assertNotNull(response);
        assertEquals("token123", response.getToken());
        verify(shareLinkRepository).save(any(ShareLink.class));
    }

    @Test
    void accessShareLink_success() {
        File file = File.builder().id(10L).s3Key("s3/key").originalName("test.txt").build();
        ShareLink link = ShareLink.builder()
                .token("token123")
                .file(file)
                .passwordHash("hashed-secret")
                .useCount(0)
                .maxUses(5)
                .isActive(true)
                .build();
                
        when(shareLinkRepository.findByToken("token123")).thenReturn(Optional.of(link));
        when(passwordEncoder.matches("secret", "hashed-secret")).thenReturn(true);
        when(s3Service.generatePresignedGetUrl(eq("s3/key"), eq("test.txt"), eq(5L)))
                .thenReturn("https://s3.url");
                
        String url = shareLinkService.accessShareLink("token123", "secret");
        
        assertEquals("https://s3.url", url);
        assertEquals(1, link.getUseCount());
        verify(shareLinkRepository).save(link);
    }
    
    @Test
    void accessShareLink_expired_throwsException() {
        ShareLink link = ShareLink.builder()
                .token("token123")
                .isActive(true)
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
                
        when(shareLinkRepository.findByToken("token123")).thenReturn(Optional.of(link));
        
        assertThrows(AccessDeniedException.class, () -> 
                shareLinkService.accessShareLink("token123", null));
    }
    
    @Test
    void accessShareLink_wrongPassword_throwsException() {
        ShareLink link = ShareLink.builder()
                .token("token123")
                .isActive(true)
                .passwordHash("hashed-secret")
                .build();
                
        when(shareLinkRepository.findByToken("token123")).thenReturn(Optional.of(link));
        when(passwordEncoder.matches("wrong", "hashed-secret")).thenReturn(false);
        
        assertThrows(AccessDeniedException.class, () -> 
                shareLinkService.accessShareLink("token123", "wrong"));
    }
    
    @Test
    void accessShareLink_maxUsesReached_throwsException() {
        ShareLink link = ShareLink.builder()
                .token("token123")
                .isActive(true)
                .useCount(5)
                .maxUses(5)
                .build();
                
        when(shareLinkRepository.findByToken("token123")).thenReturn(Optional.of(link));
        
        assertThrows(AccessDeniedException.class, () -> 
                shareLinkService.accessShareLink("token123", null));
    }
}
