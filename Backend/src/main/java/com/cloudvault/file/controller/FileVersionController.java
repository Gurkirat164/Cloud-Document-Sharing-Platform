package com.cloudvault.file.controller;

import com.cloudvault.common.response.ApiResponse;
import com.cloudvault.common.security.CurrentUser;
import com.cloudvault.domain.User;
import com.cloudvault.file.dto.FileVersionResponse;
import com.cloudvault.file.dto.RestoreVersionRequest;
import com.cloudvault.file.service.FileVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing file versioning endpoints.
 *
 * <p>All endpoints are nested under {@code /api/files/{fileUuid}/versions}.
 * Access control rules:
 * <ul>
 *   <li>GET history — file owner, ADMIN, or any user with an active permission.</li>
 *   <li>POST restore, DELETE version — file owner or ADMIN only.</li>
 * </ul>
 *
 * <p>PREREQUISITE: S3 bucket versioning must be enabled on cloudvault-dev-files
 * before any versioning endpoint can return meaningful data.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File Versions", description = "APIs for managing file version history")
public class FileVersionController {

    private final FileVersionService fileVersionService;

    /**
     * GET /api/files/{fileUuid}/versions
     *
     * <p>Returns the full version history for a file, sorted newest-first.
     *
     * @param fileUuid    public UUID of the file
     * @param currentUser authenticated caller
     * @return 200 OK with list of {@link FileVersionResponse} wrapped in {@link ApiResponse}
     */
    @GetMapping("/{fileUuid}/versions")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get version history for a file",
            description = "Returns all recorded versions, newest first. "
                        + "Access: owner, ADMIN, or any user with an active share permission."
    )
    public ResponseEntity<ApiResponse<List<FileVersionResponse>>> getVersionHistory(
            @PathVariable String fileUuid,
            @CurrentUser User currentUser) {

        List<FileVersionResponse> versions =
                fileVersionService.getVersionHistory(fileUuid, currentUser);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    /**
     * POST /api/files/{fileUuid}/versions/restore
     *
     * <p>Creates a new S3 version that has the same content as the specified
     * historical version, making it the new current version.
     *
     * @param fileUuid    public UUID of the file to restore
     * @param request     body containing the {@code versionNumber} to restore
     * @param currentUser authenticated caller (must be owner or ADMIN)
     * @return 200 OK with the {@link FileVersionResponse} of the newly created restore version
     */
    @PostMapping("/{fileUuid}/versions/restore")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary     = "Restore a file to a previous version",
            description = "Copies the content of the specified version to a new S3 version, "
                        + "making it the current version. Access: owner or ADMIN only."
    )
    public ResponseEntity<ApiResponse<FileVersionResponse>> restoreVersion(
            @PathVariable String fileUuid,
            @Valid @RequestBody RestoreVersionRequest request,
            @CurrentUser User currentUser) {

        FileVersionResponse restored =
                fileVersionService.restoreVersion(fileUuid, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(
                "File restored to version " + request.getVersionNumber(), restored));
    }

    /**
     * DELETE /api/files/{fileUuid}/versions/{versionNumber}
     *
     * <p>Permanently deletes a specific non-current version from both S3 and the DB.
     * The current version cannot be deleted via this endpoint.
     *
     * @param fileUuid      public UUID of the file
     * @param versionNumber 1-based sequential version number to delete permanently
     * @param currentUser   authenticated caller (must be owner or ADMIN)
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{fileUuid}/versions/{versionNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary     = "Permanently delete a specific file version",
            description = "Cannot delete the current active version. "
                        + "Permanently removes both the S3 object version and the DB record. "
                        + "Access: owner or ADMIN only."
    )
    public ResponseEntity<Void> deleteVersion(
            @PathVariable String fileUuid,
            @PathVariable int versionNumber,
            @CurrentUser User currentUser) {

        fileVersionService.deleteVersion(fileUuid, versionNumber, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/files/{fileUuid}/versions/count
     *
     * <p>Returns the total number of recorded versions for a file.
     * Useful for the UI to decide whether to show the version history button.
     *
     * @param fileUuid public UUID of the file
     * @return 200 OK with the integer count wrapped in {@link ApiResponse}
     */
    @GetMapping("/{fileUuid}/versions/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get total number of versions for a file",
            description = "Returns the count of all recorded version entries (including non-current)."
    )
    public ResponseEntity<ApiResponse<Integer>> getVersionCount(
            @PathVariable String fileUuid) {

        int count = fileVersionService.getVersionCount(fileUuid);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
