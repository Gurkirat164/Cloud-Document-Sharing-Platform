package com.cloudvault.file.controller;

import com.cloudvault.common.response.ApiResponse;
import com.cloudvault.common.response.PagedResponse;
import com.cloudvault.common.security.CurrentUser;
import com.cloudvault.domain.User;
import com.cloudvault.file.dto.FileMetadataRequest;
import com.cloudvault.file.dto.FileResponse;
import com.cloudvault.file.dto.FileSearchRequest;
import com.cloudvault.file.dto.PresignedUrlResponse;
import com.cloudvault.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the file upload flow:
 *
 * <pre>
 *   GET    /api/files/presigned-url  — generate S3 presigned PUT URL
 *   POST   /api/files                — save file metadata after S3 upload
 *   GET    /api/files                — list authenticated user's files
 *   DELETE /api/files/{uuid}         — soft-delete a file
 *   GET    /api/files/search         — search &amp; filter files
 *   GET    /api/files/recent         — 5 most recently uploaded files
 *   GET    /api/files/stats          — storage usage statistics
 * </pre>
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload, listing, search, and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileService fileService;

    // ── Existing endpoints — unchanged ───────────────────────────────────────

    /**
     * GET /api/files/presigned-url?filename=report.pdf&contentType=application/pdf&fileSize=1048576
     *
     * <p>Returns a short-lived S3 presigned PUT URL so the browser can upload directly.
     * Performs an early quota pre-check using {@code fileSize} to give immediate feedback
     * before any bytes are sent to S3. The definitive check still runs in
     * {@code POST /api/files} which saves the metadata.
     *
     * @param fileSize declared size in bytes of the file about to be uploaded
     */
    @GetMapping("/presigned-url")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary   = "Get presigned upload URL",
        description = "Generates a 15-minute presigned PUT URL for direct browser → S3 upload. "
                    + "Performs an early storage quota check. Returns the URL and the S3 key to "
                    + "include in the subsequent metadata save."
    )
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @RequestParam @NotBlank String filename,
            @RequestParam @NotBlank String contentType,
            @RequestParam long fileSize,
            @CurrentUser User user) {

        PresignedUrlResponse response =
                fileService.getPresignedUploadUrl(filename, contentType, fileSize, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/files
     * Called after the frontend has successfully PUT the file to S3.
     * Persists file metadata to MySQL and increments the user's storage counter.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary     = "Save file metadata",
        description = "Persists file metadata (name, size, MIME type, S3 key) after a successful "
                    + "direct S3 upload. Increments the owner's storage usage."
    )
    public ResponseEntity<ApiResponse<FileResponse>> saveFileMetadata(
            @Valid @RequestBody FileMetadataRequest request,
            @CurrentUser User user) {

        FileResponse response = fileService.saveFileMetadata(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully", response));
    }

    /**
     * GET /api/files
     * Returns a paginated list of all non-deleted files owned by the authenticated user.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary     = "List my files",
        description = "Returns a paginated list of non-deleted files owned by the authenticated user."
    )
    public ResponseEntity<ApiResponse<PagedResponse<FileResponse>>> listFiles(
            @PageableDefault(size = 20, sort = "uploadedAt") Pageable pageable,
            @CurrentUser User user) {

        Page<FileResponse> page = fileService.getUserFiles(user, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(page)));
    }

    /**
     * DELETE /api/files/{uuid}
     * Soft-deletes the file identified by its public UUID.
     * Only the owner may delete their own file.
     */
    @DeleteMapping("/{uuid}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary     = "Delete a file",
        description = "Soft-deletes a file by its UUID. Only the owner can perform this action. "
                    + "The S3 object remains until the async cleanup job runs."
    )
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable String uuid,
            @CurrentUser User user) {

        fileService.deleteFile(uuid, user);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    // ── New endpoints ────────────────────────────────────────────────────────

    /**
     * GET /api/files/search
     *
     * <p>All filter parameters are passed as query-string values and bound via
     * {@code @ModelAttribute}. No request body is used.
     *
     * <p>Example:
     * {@code GET /api/files/search?query=report&mimeTypeCategory=application&sortBy=uploadedAt&sortDirection=DESC&page=0&size=20}
     *
     * @param request     filter and pagination parameters (all optional)
     * @param currentUser authenticated caller
     * @return 200 OK with a paginated {@link PagedResponse} of {@link FileResponse}
     */
    @Operation(
        summary     = "Search and filter files",
        description = "Search by name, filter by MIME type, date range, or file size. "
                    + "Supports sorting and pagination. Non-admin users always see only their own files."
    )
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<FileResponse>>> searchFiles(
            @Valid @ModelAttribute FileSearchRequest request,
            @CurrentUser User currentUser) {

        PagedResponse<FileResponse> result = fileService.searchFiles(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/files/recent
     *
     * <p>Returns the 5 most recently uploaded non-deleted files for the authenticated user.
     * Intended for use by the dashboard widget.
     *
     * @param currentUser authenticated caller
     * @return 200 OK with a list of up to 5 {@link FileResponse} DTOs
     */
    @Operation(summary = "Get 5 most recently uploaded files")
    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FileResponse>>> getRecentFiles(
            @CurrentUser User currentUser) {

        List<FileResponse> recent = fileService.getRecentFiles(currentUser);
        return ResponseEntity.ok(ApiResponse.success(recent));
    }

    /**
     * GET /api/files/stats
     *
     * <p>Returns storage usage statistics for the authenticated user, including
     * total file count, bytes used, quota, percentage used, and a formatted size string.
     *
     * @param currentUser authenticated caller
     * @return 200 OK with a flat {@code Map<String, Object>} of stats
     */
    @Operation(summary = "Get storage usage stats for current user")
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFileStats(
            @CurrentUser User currentUser) {

        Map<String, Object> stats = fileService.getFileStats(currentUser);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
