package com.cloudvault.file;

import com.cloudvault.domain.File;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.Role;
import com.cloudvault.file.dto.FileSearchRequest;
import com.cloudvault.file.repository.FileRepository;
import com.cloudvault.file.specification.FileSpecification;
import com.cloudvault.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link FileSpecification} using a real JPA environment
 * and the application's actual database dialect.
 *
 * <p>{@code @DataJpaTest} loads only the JPA slice (entities, repositories,
 * {@link TestEntityManager}). {@code @AutoConfigureTestDatabase(replace = NONE)}
 * tells Spring Boot NOT to replace the configured DataSource with an embedded
 * H2 database — the tests run against a real MySQL instance supplied through
 * Testcontainers (configured in {@code application-test.yml} or via Spring Boot
 * auto-configuration of the {@code @Testcontainers} extension).
 *
 * <p>Each test persists its own fixtures through {@link TestEntityManager} so
 * tests are independent. All changes are rolled back after each test because
 * {@code @DataJpaTest} runs each test in a transaction that is always rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FileSpecificationTest {

    @Autowired private TestEntityManager em;
    @Autowired private FileRepository   fileRepository;
    @Autowired private UserRepository   userRepository;

    private User owner;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // Persist a shared owner user for all tests in this class.
        owner = userRepository.save(User.builder()
                .email("spec-owner@example.com")
                .fullName("Spec Owner")
                .passwordHash("$2a$10$hash")
                .role(Role.USER)
                .storageUsed(0L)
                .storageQuota(5_368_709_120L)
                .isActive(true)
                .build());
        em.flush();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private File persistFile(String name, String mimeType, long sizeBytes, boolean deleted) {
        File f = File.builder()
                .owner(owner)
                .originalName(name)
                .s3Key("test/" + name)
                .s3Bucket("cloudvault")
                .mimeType(mimeType)
                .sizeBytes(sizeBytes)
                .isDeleted(deleted)
                .build();
        return em.persistFlushFind(f);
    }

    // ── Test 1: nameLike_caseInsensitive_findsMatch ───────────────────────────

    @Test
    @DisplayName("nameLike: case-insensitive partial match returns expected file")
    void nameLike_caseInsensitive_findsMatch() {
        persistFile("Annual_Report_2024.pdf", "application/pdf", 1024L, false);
        persistFile("invoice_march.pdf",      "application/pdf", 512L,  false);
        persistFile("photo.jpg",              "image/jpeg",       2048L, false);

        Specification<File> spec = FileSpecification.nameLike("ANNUAL");

        List<File> results = fileRepository.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getOriginalName()).isEqualToIgnoringCase("Annual_Report_2024.pdf");
    }

    // ── Test 2: hasMimeTypeCategory_image_findsOnlyImages ────────────────────

    @Test
    @DisplayName("hasMimeTypeCategory: 'image' category returns only image/* files")
    void hasMimeTypeCategory_image_findsOnlyImages() {
        persistFile("photo.jpg",   "image/jpeg",       1024L, false);
        persistFile("icon.png",    "image/png",        512L,  false);
        persistFile("report.pdf",  "application/pdf",  4096L, false);
        persistFile("video.mp4",   "video/mp4",        204800L, false);

        Specification<File> spec = FileSpecification.hasOwner(owner.getId())
                .and(FileSpecification.hasMimeTypeCategory("image"));

        List<File> results = fileRepository.findAll(spec);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(f -> f.getMimeType().startsWith("image/"));
    }

    // ── Test 3: uploadedAfter_filtersCorrectly ────────────────────────────────

    @Test
    @DisplayName("uploadedAfter: filters out files uploaded before the cutoff date")
    void uploadedAfter_filtersCorrectly() {
        // Files are inserted now; we will query from (now - 1 minute) and before (now - 1 hour).
        File recent = persistFile("recent.txt", "text/plain", 100L, false);
        em.flush(); // ensure uploadedAt is set by @PrePersist

        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);

        Specification<File> spec = FileSpecification.hasOwner(owner.getId())
                .and(FileSpecification.uploadedAfter(oneHourAgo));

        List<File> results = fileRepository.findAll(spec);

        // The recently persisted file should be found; a query for "after 2 hours ago" should also work
        assertThat(results).extracting(File::getOriginalName).contains("recent.txt");

        // Now query for files uploaded after "now" — should find nothing
        Specification<File> futureSpec = FileSpecification.hasOwner(owner.getId())
                .and(FileSpecification.uploadedAfter(Instant.now().plus(1, ChronoUnit.HOURS)));
        assertThat(fileRepository.findAll(futureSpec)).isEmpty();
    }

    // ── Test 4: sizeRange_minAndMax_filtersCorrectly ──────────────────────────

    @Test
    @DisplayName("sizeRange: minSize + maxSize filter returns only files within range")
    void sizeRange_minAndMax_filtersCorrectly() {
        persistFile("tiny.txt",    "text/plain",       100L,    false);  // too small
        persistFile("medium.pdf",  "application/pdf",  5_000L,  false);  // in range
        persistFile("large.zip",   "application/zip",  100_000L, false); // too large

        Specification<File> spec = FileSpecification.hasOwner(owner.getId())
                .and(FileSpecification.sizeGreaterThan(1_000L))
                .and(FileSpecification.sizeLessThan(10_000L));

        List<File> results = fileRepository.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getOriginalName()).isEqualTo("medium.pdf");
    }

    // ── Test 5: isNotDeleted_excludesSoftDeleted ──────────────────────────────

    @Test
    @DisplayName("isNotDeleted: soft-deleted files are excluded from results")
    void isNotDeleted_excludesSoftDeleted() {
        persistFile("active.pdf",  "application/pdf", 1024L, false);
        persistFile("deleted.txt", "text/plain",       512L, true);

        Specification<File> spec = FileSpecification.hasOwner(owner.getId())
                .and(FileSpecification.isNotDeleted());

        List<File> results = fileRepository.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getOriginalName()).isEqualTo("active.pdf");
    }

    // ── Test 6: buildFromRequest_composesAllFilters ───────────────────────────

    @Test
    @DisplayName("buildFromRequest: all active filters compose correctly and return matching files only")
    void buildFromRequest_composesAllFilters() {
        persistFile("Annual_Report.pdf",    "application/pdf",  2_048L, false);
        persistFile("Annual_Budget.xlsx",   "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 1_024L, false);
        persistFile("photo.jpg",            "image/jpeg",        512L,  false);
        persistFile("deleted_report.pdf",   "application/pdf",  2_048L, true);

        FileSearchRequest request = new FileSearchRequest();
        request.setQuery("annual");           // name filter
        request.setMimeType("application/pdf"); // exact MIME
        // Sort and pagination defaults are fine

        Specification<File> spec = FileSpecification.buildFromRequest(request, owner.getId(), false);
        List<File> results = fileRepository.findAll(spec);

        // Should return only Annual_Report.pdf — not the xlsx or deleted pdf
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getOriginalName()).isEqualTo("Annual_Report.pdf");
    }

    // ── Test 7: buildFromRequest_adminWithIncludeDeleted_includesSoftDeleted ──

    @Test
    @DisplayName("buildFromRequest_adminWithIncludeDeleted: admin can include soft-deleted files")
    void buildFromRequest_adminWithIncludeDeleted_includesSoftDeleted() {
        persistFile("active.pdf",  "application/pdf", 1024L, false);
        persistFile("deleted.pdf", "application/pdf", 2048L, true);

        FileSearchRequest request = new FileSearchRequest();
        request.setIncludeDeleted(true); // admin requesting deleted files

        // Admin calling with ownerId = owner.getId() (scoping to one user still)
        Specification<File> spec = FileSpecification.buildFromRequest(request, owner.getId(), true);
        List<File> results = fileRepository.findAll(spec);

        // Both files (active + deleted) should be returned
        assertThat(results).hasSize(2);
        assertThat(results).extracting(File::getOriginalName)
                .containsExactlyInAnyOrder("active.pdf", "deleted.pdf");
    }
}
