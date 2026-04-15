package com.cloudvault.file.controller;

import com.cloudvault.common.response.ApiResponse;
import com.cloudvault.common.response.PagedResponse;
import com.cloudvault.common.security.CurrentUser;
import com.cloudvault.domain.User;
import com.cloudvault.file.dto.FileMetadataRequest;
import com.cloudvault.file.dto.FileResponse;
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

/**
 * REST endpoints for the file upload flow:
 *
 * <pre>
 *   GET  /api/v1/api/files/presigned-url  — generate S3 presigned PUT URL
 *   POST /api/v1/api/files                — save file metadata after S3 upload
 *   GET  /api/v1/api/files                — list authenticated user's files
 *   DELETE /api/v1/api/files/{uuid}       — soft-delete a file
 * </pre>
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload, listing, and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileService fileService;

    /**
     * GET /api/files/presigned-url?filename=report.pdf&contentType=application/pdf
     * Returns a short-lived S3 presigned PUT URL so the browser can upload directly.
     */
    @GetMapping("/presigned-url")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary   = "Get presigned upload URL",
        description = "Generates a 15-minute presigned PUT URL for direct browser → S3 upload. "
                    + "Returns the URL and the S3 key to include in the subsequent metadata save."
    )
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @RequestParam @NotBlank String filename,
            @RequestParam @NotBlank String contentType,
            @CurrentUser User user) {

        PresignedUrlResponse response = fileService.getPresignedUploadUrl(filename, contentType, user);
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
}
