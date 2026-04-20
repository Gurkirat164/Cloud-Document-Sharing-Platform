package com.cloudvault.file;

import com.cloudvault.common.exception.AccessDeniedException;
import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.domain.File;
import com.cloudvault.domain.FileVersion;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.Role;
import com.cloudvault.file.dto.FileVersionResponse;
import com.cloudvault.file.dto.RestoreVersionRequest;
import com.cloudvault.file.repository.FileRepository;
import com.cloudvault.file.repository.FileVersionRepository;
import com.cloudvault.file.service.FileVersionService;
import com.cloudvault.file.service.S3Service;
import com.cloudvault.activity.service.ActivityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FileVersionService}.
 *
 * <p>All Spring infrastructure is replaced with Mockito doubles. Each test
 * targets one specific behaviour or precondition check in the service.
 */
@ExtendWith(MockitoExtension.class)
class FileVersionServiceTest {

    @Mock private FileVersionRepository fileVersionRepository;
    @Mock private FileRepository        fileRepository;
    @Mock private S3Service             s3Service;
    @Mock private ActivityLogService    activityLogService;

    private FileVersionService service;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static User owner() {
        return User.builder()
                .id(1L).uuid("owner-uuid").email("owner@example.com")
                .fullName("Owner User").role(Role.USER)
                .storageUsed(0L).storageQuota(5_368_709_120L).isActive(true).build();
    }

    private static User admin() {
        return User.builder()
                .id(99L).uuid("admin-uuid").email("admin@example.com")
                .fullName("Admin User").role(Role.ADMIN)
                .storageUsed(0L).storageQuota(5_368_709_120L).isActive(true).build();
    }

    private static User stranger() {
        return User.builder()
                .id(2L).uuid("stranger-uuid").email("stranger@example.com")
                .fullName("Stranger").role(Role.USER)
                .storageUsed(0L).storageQuota(5_368_709_120L).isActive(true).build();
    }

    private static File file(User owner) {
        return File.builder()
                .id(10L).uuid("file-uuid-001")
                .owner(owner).originalName("report.pdf")
                .s3Key("users/owner-uuid/report.pdf").s3Bucket("cloudvault")
                .mimeType("application/pdf").sizeBytes(1_048_576L)
                .isDeleted(false).build();
    }

    private static File deletedFile(User owner) {
        File f = file(owner);
        f.setIsDeleted(true);
        return f;
    }

    private static FileVersion version(File file, int num, boolean current) {
        return FileVersion.builder()
                .id((long) num).file(file)
                .versionNumber(num).s3VersionId("s3vid-" + num)
                .s3Key(file.getS3Key()).originalName(file.getOriginalName())
                .sizeBytes(file.getSizeBytes()).mimeType(file.getMimeType())
                .uploadedBy(file.getOwner()).isCurrentVersion(current).build();
    }

    @BeforeEach
    void setUp() {
        service = new FileVersionService(
                fileVersionRepository, fileRepository, s3Service, activityLogService);
    }

    // ── recordNewVersion ──────────────────────────────────────────────────────

    @Test
    @DisplayName("recordNewVersion_firstUpload_createsVersionOne")
    void recordNewVersion_firstUpload_createsVersionOne() {
        User  u    = owner();
        File  f    = file(u);
        String vid = "s3vid-abc";

        when(fileVersionRepository.findTopByFileIdOrderByVersionNumberDesc(f.getId()))
                .thenReturn(Optional.empty()); // no versions yet
        when(fileVersionRepository.findAllByFileIdOrderByVersionNumberDesc(f.getId()))
                .thenReturn(List.of());
        when(fileVersionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fileRepository.save(any())).thenReturn(f);

        FileVersionResponse result = service.recordNewVersion(f, vid, u);

        assertThat(result.getVersionNumber()).isEqualTo(1);
        assertThat(result.getS3VersionId()).isEqualTo(vid);
        assertThat(result.isCurrentVersion()).isTrue();
    }

    @Test
    @DisplayName("recordNewVersion_secondUpload_incrementsVersionNumber")
    void recordNewVersion_secondUpload_incrementsVersionNumber() {
        User        u   = owner();
        File        f   = file(u);
        FileVersion v1  = version(f, 1, true);

        when(fileVersionRepository.findTopByFileIdOrderByVersionNumberDesc(f.getId()))
                .thenReturn(Optional.of(v1));
        when(fileVersionRepository.findAllByFileIdOrderByVersionNumberDesc(f.getId()))
                .thenReturn(List.of(v1));
        when(fileVersionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fileRepository.save(any())).thenReturn(f);

        FileVersionResponse result = service.recordNewVersion(f, "s3vid-new", u);

        assertThat(result.getVersionNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("recordNewVersion_setsAllPreviousVersionsToNotCurrent")
    void recordNewVersion_setsAllPreviousVersionsToNotCurrent() {
        User        u  = owner();
        File        f  = file(u);
        FileVersion v1 = version(f, 1, true);
        FileVersion v2 = version(f, 2, false);

        when(fileVersionRepository.findTopByFileIdOrderByVersionNumberDesc(f.getId()))
                .thenReturn(Optional.of(v2));
        when(fileVersionRepository.findAllByFileIdOrderByVersionNumberDesc(f.getId()))
                .thenReturn(List.of(v2, v1));
        when(fileVersionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fileRepository.save(any())).thenReturn(f);

        service.recordNewVersion(f, "s3vid-v3", u);

        // All existing versions must have been flipped to false before save
        assertThat(v1.isCurrentVersion()).isFalse();
        assertThat(v2.isCurrentVersion()).isFalse();
        verify(fileVersionRepository).saveAll(anyList());
    }

    // ── getVersionHistory — access control ────────────────────────────────────

    @Test
    @DisplayName("getVersionHistory_notOwnerNotShared_throwsAccessDeniedException")
    void getVersionHistory_notOwnerNotShared_throwsAccessDeniedException() {
        User stranger = stranger();
        User owner    = owner();
        File f        = file(owner);

        when(fileRepository.findByUuid("file-uuid-001")).thenReturn(Optional.of(f));
        // Stranger has no active permission
        when(fileVersionRepository.existsActivePermission(f.getId(), stranger.getId()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getVersionHistory("file-uuid-001", stranger))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ── restoreVersion ────────────────────────────────────────────────────────

    @Test
    @DisplayName("restoreVersion_success_createsNewVersion")
    void restoreVersion_success_createsNewVersion() {
        User        u   = owner();
        File        f   = file(u);
        FileVersion v1  = version(f, 1, false); // old version to restore
        FileVersion v2  = version(f, 2, true);  // current version

        when(fileRepository.findByUuid("file-uuid-001")).thenReturn(Optional.of(f));
        when(fileVersionRepository.findByFileIdAndVersionNumber(f.getId(), 1))
                .thenReturn(Optional.of(v1));
        when(s3Service.copyVersion(v1.getS3Key(), v1.getS3VersionId(), f.getS3Key()))
                .thenReturn("s3vid-restored");
        when(fileVersionRepository.findTopByFileIdOrderByVersionNumberDesc(f.getId()))
                .thenReturn(Optional.of(v2));
        when(fileVersionRepository.findAllByFileIdOrderByVersionNumberDesc(f.getId()))
                .thenReturn(List.of(v2, v1));
        when(fileVersionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fileRepository.save(any())).thenReturn(f);

        RestoreVersionRequest req = new RestoreVersionRequest();
        req.setVersionNumber(1);

        FileVersionResponse result = service.restoreVersion("file-uuid-001", req, u);

        assertThat(result.getVersionNumber()).isEqualTo(3);
        assertThat(result.getRestoredFromVersion()).isEqualTo(1);
        assertThat(result.getS3VersionId()).isEqualTo("s3vid-restored");
        verify(activityLogService).log(eq(u), eq(f), any(), isNull());
    }

    @Test
    @DisplayName("restoreVersion_alreadyCurrentVersion_throwsIllegalArgumentException")
    void restoreVersion_alreadyCurrentVersion_throwsIllegalArgumentException() {
        User        u  = owner();
        File        f  = file(u);
        FileVersion v1 = version(f, 1, true); // already current

        when(fileRepository.findByUuid("file-uuid-001")).thenReturn(Optional.of(f));
        when(fileVersionRepository.findByFileIdAndVersionNumber(f.getId(), 1))
                .thenReturn(Optional.of(v1));

        RestoreVersionRequest req = new RestoreVersionRequest();
        req.setVersionNumber(1);

        assertThatThrownBy(() -> service.restoreVersion("file-uuid-001", req, u))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already the current version");
    }

    @Test
    @DisplayName("restoreVersion_deletedFile_throwsIllegalStateException")
    void restoreVersion_deletedFile_throwsIllegalStateException() {
        User u = owner();
        File f = deletedFile(u);

        when(fileRepository.findByUuid("file-uuid-001")).thenReturn(Optional.of(f));

        RestoreVersionRequest req = new RestoreVersionRequest();
        req.setVersionNumber(1);

        assertThatThrownBy(() -> service.restoreVersion("file-uuid-001", req, u))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deleted file");
    }

    @Test
    @DisplayName("restoreVersion_versionNotFound_throwsResourceNotFoundException")
    void restoreVersion_versionNotFound_throwsResourceNotFoundException() {
        User u = owner();
        File f = file(u);

        when(fileRepository.findByUuid("file-uuid-001")).thenReturn(Optional.of(f));
        when(fileVersionRepository.findByFileIdAndVersionNumber(f.getId(), 99))
                .thenReturn(Optional.empty());

        RestoreVersionRequest req = new RestoreVersionRequest();
        req.setVersionNumber(99);

        assertThatThrownBy(() -> service.restoreVersion("file-uuid-001", req, u))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteVersion ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteVersion_currentVersion_throwsIllegalArgumentException")
    void deleteVersion_currentVersion_throwsIllegalArgumentException() {
        User        u  = owner();
        File        f  = file(u);
        FileVersion v1 = version(f, 1, true); // current — cannot delete

        when(fileRepository.findByUuid("file-uuid-001")).thenReturn(Optional.of(f));
        when(fileVersionRepository.findByFileIdAndVersionNumber(f.getId(), 1))
                .thenReturn(Optional.of(v1));

        assertThatThrownBy(() -> service.deleteVersion("file-uuid-001", 1, u))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete the current version");
    }

    @Test
    @DisplayName("deleteVersion_success_removesFromDbAndS3")
    void deleteVersion_success_removesFromDbAndS3() {
        User        u  = owner();
        File        f  = file(u);
        FileVersion v1 = version(f, 1, false); // non-current — safe to delete

        when(fileRepository.findByUuid("file-uuid-001")).thenReturn(Optional.of(f));
        when(fileVersionRepository.findByFileIdAndVersionNumber(f.getId(), 1))
                .thenReturn(Optional.of(v1));
        doNothing().when(s3Service).deleteVersion(v1.getS3Key(), v1.getS3VersionId());

        service.deleteVersion("file-uuid-001", 1, u);

        verify(s3Service).deleteVersion(v1.getS3Key(), v1.getS3VersionId());
        verify(fileVersionRepository).delete(v1);
    }
}
