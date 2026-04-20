package com.cloudvault.file.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for the version restore endpoint.
 *
 * <p>Identifies which version number should become the new current version.
 * The target file is always identified by the path variable {@code fileUuid},
 * not by a field in this request body.
 */
@Getter
@Setter
@NoArgsConstructor
public class RestoreVersionRequest {

    /**
     * The 1-based sequential version number to restore.
     * Must be a positive integer and refers to an existing non-current version.
     */
    @NotNull(message = "Version number is required")
    @Min(value = 1, message = "Version number must be at least 1")
    private Integer versionNumber;
}
