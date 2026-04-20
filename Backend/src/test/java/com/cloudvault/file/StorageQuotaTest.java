package com.cloudvault.file;

import com.cloudvault.common.exception.StorageQuotaExceededException;
import com.cloudvault.domain.File;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.Role;
import com.cloudvault.file.dto.FileMetadataRequest;
import com.cloudvault.file.dto.FileResponse;
import com.cloudvault.file.dto.FileSearchRequest;
import com.cloudvault.file.repository.FileRepository;
import com.cloudvault.file.service.FileService;
import com.cloudvault.file.service.S3Service;
import com.cloudvault.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for storage quota enforcement in {@link FileService}.
 *
 * <p>All Spring infrastructure is replaced with Mockito doubles so each test
 * executes in a pure JVM without an embedded container or database.
 */
@ExtendWith(MockitoExtension.class)
class StorageQuotaTest {

    @Mock private FileRepository fileRepository;
    @Mock private UserRepository userRepository;
    @Mock private S3Service      s3Service;

    private FileService fileService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    /** 5 GB quota, 3 GB used → 2 GB remaining. */
    private static User buildUser(long storageUsed, long storageQuota) {
        return User.builder()
                .id(1L)
                .uuid("test-user-uuid")
                .email("alice@example.com")
                .fullName("Alice")
                .passwordHash("$2a$10$hash")
                .role(Role.USER)
                .storageUsed(storageUsed)
                .storageQuota(storageQuota)
                .isActive(true)
                .build();
    }

    private static File buildFile(User owner, long sizeBytes) {
        return File.builder()
                .id(10L)
                .uuid("file-uuid-001")
                .originalName("report.pdf")
                .s3Key("users/" + owner.getUuid() + "/report.pdf")
                .s3Bucket("cloudvault")
                .mimeType("application/pdf")
                .sizeBytes(sizeBytes)
                .isDeleted(false)
                .owner(owner)
                .build();
    }

    @BeforeEach
    void setUp() {
        fileService = new FileService(s3Service, fileRepository, userRepository);
        ReflectionTestUtils.setField(fileService, "bucketName", "cloudvault-test");
    }

    // ── Test 1: checkStorageQuota_withinLimit_doesNotThrow ────────────────────

    @Test
    @DisplayName("checkStorageQuota_withinLimit: file smaller than remaining space — no exception")
    void checkStorageQuota_withinLimit_doesNotThrow() {
        // 3 GB used, 5 GB quota → 2 GB remaining
        User user = buildUser(3_221_225_472L, 5_368_709_120L);

        FileMetadataRequest request = new FileMetadataRequest(
                "small.pdf", "s3/key", "application/pdf", 1_048_576L, null); // 1 MB

        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Must not throw
        assertThatCode(() -> fileService.saveFileMetadata(request, user))
                .doesNotThrowAnyException();
    }

    // ── Test 2: checkStorageQuota_exactlyAtLimit_doesNotThrow ────────────────

    @Test
    @DisplayName("checkStorageQuota_exactlyAtLimit: file exactly fills remaining space — no exception")
    void checkStorageQuota_exactlyAtLimit_doesNotThrow() {
        long quota = 5_368_709_120L; // 5 GB
        long used  = 4_294_967_296L; // 4 GB  →  1 GB remaining
        long fileSize = quota - used; // exactly 1 GB

        User user = buildUser(used, quota);

        FileMetadataRequest request = new FileMetadataRequest(
                "exact.bin", "s3/key", "application/octet-stream", fileSize, null);

        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Exactly filling the quota is allowed (fileSizeBytes == remainingBytes)
        assertThatCode(() -> fileService.saveFileMetadata(request, user))
                .doesNotThrowAnyException();
    }

    // ── Test 3: checkStorageQuota_exceedsLimit_throwsStorageQuotaExceededException ─

    @Test
    @DisplayName("checkStorageQuota_exceedsLimit: file larger than remaining space — throws 413 exception")
    void checkStorageQuota_exceedsLimit_throwsStorageQuotaExceededException() {
        long quota = 5_368_709_120L; // 5 GB
        long used  = 4_900_000_000L; // ~4.56 GB used → ~469 MB remaining
        long fileSize = 1_000_000_000L; // 1 GB — exceeds remaining

        User user = buildUser(used, quota);

        FileMetadataRequest request = new FileMetadataRequest(
                "large.iso", "s3/key", "application/octet-stream", fileSize, null);

        assertThatThrownBy(() -> fileService.saveFileMetadata(request, user))
                .isInstanceOf(StorageQuotaExceededException.class)
                .satisfies(ex -> {
                    StorageQuotaExceededException sqe = (StorageQuotaExceededException) ex;
                    assertThat(sqe.getUsedBytes()).isEqualTo(used);
                    assertThat(sqe.getQuotaBytes()).isEqualTo(quota);
                    assertThat(sqe.getRequestedBytes()).isEqualTo(fileSize);
                    assertThat(sqe.getMessage()).contains("MB");
                });
    }

    // ── Test 4: saveFileMetadata_quotaExceeded_throwsBeforeDbWrite ──────────

    @Test
    @DisplayName("saveFileMetadata_quotaExceeded: file record must NOT be saved when quota is exceeded")
    void saveFileMetadata_quotaExceeded_throwsBeforeDbWrite() {
        User user = buildUser(4_900_000_000L, 5_368_709_120L); // ~469 MB remaining

        FileMetadataRequest request = new FileMetadataRequest(
                "toobig.zip", "s3/key", "application/zip", 1_000_000_000L, null); // 1 GB

        assertThatThrownBy(() -> fileService.saveFileMetadata(request, user))
                .isInstanceOf(StorageQuotaExceededException.class);

        // Key assertion: no File entity was persisted to the DB
        verify(fileRepository, never()).save(any(File.class));
        // And the storage counter was never updated
        verify(userRepository, never()).save(any(User.class));
    }

    // ── Test 5: deleteFile_decrementsStorageUsed ──────────────────────────────

    @Test
    @DisplayName("deleteFile: storageUsed is decremented by the file's size after soft-delete")
    void deleteFile_decrementsStorageUsed() {
        long initialUsed = 3_000_000L;
        long fileSize    = 1_048_576L; // 1 MB

        User user = buildUser(initialUsed, 5_368_709_120L);
        File file = buildFile(user, fileSize);

        when(fileRepository.findByUuidAndIsDeletedFalse("file-uuid-001"))
                .thenReturn(Optional.of(file));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fileService.deleteFile("file-uuid-001", user);

        // Verify the User was saved with the decremented counter
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        long savedUsed = userCaptor.getValue().getStorageUsed();
        assertThat(savedUsed).isEqualTo(initialUsed - fileSize);
    }

    // ── Test 6: decrementStorageUsed_neverGoesBelowZero ──────────────────────

    @Test
    @DisplayName("decrementStorageUsed: Math.max(0,...) prevents negative storage counter")
    void decrementStorageUsed_neverGoesBelowZero() {
        // User currently has only 100 bytes used, but file claims it's 2 MB
        long initialUsed = 100L;
        long fileSize    = 2_097_152L; // 2 MB — larger than storageUsed

        User user = buildUser(initialUsed, 5_368_709_120L);
        File file = buildFile(user, fileSize);

        when(fileRepository.findByUuidAndIsDeletedFalse("file-uuid-001"))
                .thenReturn(Optional.of(file));
        when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fileService.deleteFile("file-uuid-001", user);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        // Must be floored at 0, not negative
        assertThat(userCaptor.getValue().getStorageUsed()).isEqualTo(0L);
    }

    // ── Test 7: recalculateStorageUsed_fixesMismatch (via UserService) ────────
    //   Note: This test exercises the service layer logic for repair.
    //   Full test lives in UserServiceTest; here we verify FileService.formatBytes helpers.

    @Test
    @DisplayName("formatBytes: correctly formats B, KB, MB, GB boundaries")
    void formatBytes_correctlyFormatsAllBoundaries() {
        assertThat(FileService.formatBytes(0L))               .isEqualTo("0 B");
        assertThat(FileService.formatBytes(512L))             .isEqualTo("512 B");
        assertThat(FileService.formatBytes(1_024L))           .isEqualTo("1.0 KB");
        assertThat(FileService.formatBytes(1_048_576L))       .isEqualTo("1.0 MB");
        assertThat(FileService.formatBytes(2_621_440L))       .isEqualTo("2.5 MB");
        assertThat(FileService.formatBytes(1_073_741_824L))   .isEqualTo("1.0 GB");
        assertThat(FileService.formatBytes(5_368_709_120L))   .isEqualTo("5.0 GB");
    }
}
