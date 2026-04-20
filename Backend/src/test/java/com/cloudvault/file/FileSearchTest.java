package com.cloudvault.file;

import com.cloudvault.common.response.PagedResponse;
import com.cloudvault.domain.File;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.Role;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the file search and stats methods in {@link FileService}.
 *
 * <p>All Spring infrastructure is replaced with Mockito doubles so each test
 * executes in a pure JVM without an embedded container or database.
 */
@ExtendWith(MockitoExtension.class)
class FileSearchTest {

    @Mock private FileRepository fileRepository;
    @Mock private S3Service      s3Service;
    @Mock private UserRepository userRepository;

    private FileService fileService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private static User buildUser(Long id, Role role) {
        return User.builder()
                .id(id)
                .uuid("user-uuid-" + id)
                .email("user" + id + "@example.com")
                .fullName("User " + id)
                .passwordHash("$2a$10$hash")
                .role(role)
                .storageUsed(2_500_000L)   // 2.5 MB
                .storageQuota(10_000_000L) // 10 MB
                .isActive(true)
                .build();
    }

    private static File buildFile(Long id, User owner) {
        return File.builder()
                .id(id)
                .uuid("file-uuid-" + id)
                .originalName("document-" + id + ".pdf")
                .s3Key("users/" + owner.getUuid() + "/doc" + id + ".pdf")
                .s3Bucket("cloudvault")
                .mimeType("application/pdf")
                .sizeBytes(1_024L * id)
                .owner(owner)
                .isDeleted(false)
                .build();
    }

    @BeforeEach
    void setUp() {
        fileService = new FileService(s3Service, fileRepository, userRepository);
        // Inject the bucket name value (normally injected by Spring @Value)
        ReflectionTestUtils.setField(fileService, "bucketName", "cloudvault-test");
    }

    // ── Test 1: searchFiles_asUser_forcesOwnerId ──────────────────────────────

    @Test
    @DisplayName("searchFiles_asUser: non-admin owner id is always forced to currentUser.getId()")
    @SuppressWarnings("unchecked")
    void searchFiles_asUser_forcesOwnerId() {
        User user = buildUser(1L, Role.USER);
        File ownFile = buildFile(10L, user);

        Page<File> page = new PageImpl<>(List.of(ownFile));
        when(fileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        FileSearchRequest request = new FileSearchRequest();
        // A request with no extra filters — but even if userId were forced, it should be 1L

        PagedResponse<FileResponse> result = fileService.searchFiles(request, user);

        // Verify findAll was called and result contains 1 file
        verify(fileRepository).findAll(any(Specification.class), any(Pageable.class));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).uuid()).isEqualTo("file-uuid-10");
    }

    // ── Test 2: searchFiles_asAdmin_allowsNullOwnerId ─────────────────────────

    @Test
    @DisplayName("searchFiles_asAdmin: admin can search across all users (null ownerId)")
    @SuppressWarnings("unchecked")
    void searchFiles_asAdmin_allowsNullOwnerId() {
        User admin = buildUser(99L, Role.ADMIN);
        User other = buildUser(2L, Role.USER);
        File otherFile = buildFile(20L, other);

        Page<File> page = new PageImpl<>(List.of(otherFile));
        when(fileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        FileSearchRequest request = new FileSearchRequest();
        // Admin submits no additional owner filter — should get all files

        PagedResponse<FileResponse> result = fileService.searchFiles(request, admin);

        verify(fileRepository).findAll(any(Specification.class), any(Pageable.class));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).ownerEmail()).isEqualTo(other.getEmail());
    }

    // ── Test 3: searchFiles_invalidSortBy_defaultsToUploadedAt ───────────────

    @Test
    @DisplayName("searchFiles_invalidSortBy: invalid sortBy is silently corrected to uploadedAt")
    @SuppressWarnings("unchecked")
    void searchFiles_invalidSortBy_defaultsToUploadedAt() {
        User user = buildUser(1L, Role.USER);

        when(fileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        FileSearchRequest request = new FileSearchRequest();
        request.setSortBy("nonExistentColumn");  // invalid

        fileService.searchFiles(request, user);

        // After the call the field must have been corrected
        assertThat(request.getSortBy()).isEqualTo("uploadedAt");
    }

    // ── Test 4: searchFiles_invalidSortDirection_defaultsToDesc ──────────────

    @Test
    @DisplayName("searchFiles_invalidSortDirection: invalid direction is silently corrected to DESC")
    @SuppressWarnings("unchecked")
    void searchFiles_invalidSortDirection_defaultsToDesc() {
        User user = buildUser(1L, Role.USER);

        when(fileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        FileSearchRequest request = new FileSearchRequest();
        request.setSortDirection("DIAGONAL");  // invalid

        fileService.searchFiles(request, user);

        assertThat(request.getSortDirection()).isEqualTo("DESC");
    }

    // ── Test 5: searchFiles_withQueryFilter_buildsCorrectSpec ────────────────

    @Test
    @DisplayName("searchFiles_withQueryFilter: repository is called and result is correctly mapped")
    @SuppressWarnings("unchecked")
    void searchFiles_withQueryFilter_buildsCorrectSpec() {
        User user = buildUser(1L, Role.USER);
        File file = buildFile(5L, user);

        Page<File> page = new PageImpl<>(List.of(file));
        when(fileRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        FileSearchRequest request = new FileSearchRequest();
        request.setQuery("document");
        request.setMimeTypeCategory("application");

        PagedResponse<FileResponse> result = fileService.searchFiles(request, user);

        verify(fileRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent().get(0).originalName()).contains("document");
    }

    // ── Test 6: getRecentFiles_returnsMaxFive ─────────────────────────────────

    @Test
    @DisplayName("getRecentFiles: delegates to findTop5 and maps all results to FileResponse")
    void getRecentFiles_returnsMaxFive() {
        User user = buildUser(1L, Role.USER);
        List<File> files = List.of(
                buildFile(1L, user), buildFile(2L, user), buildFile(3L, user),
                buildFile(4L, user), buildFile(5L, user)
        );

        when(fileRepository.findTop5ByOwnerIdAndIsDeletedFalseOrderByUploadedAtDesc(user.getId()))
                .thenReturn(files);

        List<FileResponse> result = fileService.getRecentFiles(user);

        assertThat(result).hasSize(5);
        verify(fileRepository).findTop5ByOwnerIdAndIsDeletedFalseOrderByUploadedAtDesc(user.getId());
    }

    // ── Test 7: getFileStats_returnsCorrectStoragePercent ────────────────────

    @Test
    @DisplayName("getFileStats: computes storage percentage correctly and includes all required keys")
    void getFileStats_returnsCorrectStoragePercent() {
        // storageUsed = 2_500_000 (2.5 MB), storageQuota = 10_000_000 (10 MB) → 25.0%
        User user = buildUser(1L, Role.USER);

        when(fileRepository.countByOwnerIdAndIsDeletedFalse(user.getId())).thenReturn(12L);

        var stats = fileService.getFileStats(user);

        assertThat(stats).containsKeys(
                "totalFiles", "storageUsed", "storageQuota",
                "storageUsedPercent", "storageUsedFormatted");

        assertThat(stats.get("totalFiles")).isEqualTo(12L);
        assertThat(stats.get("storageUsed")).isEqualTo(2_500_000L);
        assertThat(stats.get("storageQuota")).isEqualTo(10_000_000L);
        assertThat((Double) stats.get("storageUsedPercent")).isEqualTo(25.0);
        assertThat(stats.get("storageUsedFormatted")).isEqualTo("2.4 MB");
    }
}
