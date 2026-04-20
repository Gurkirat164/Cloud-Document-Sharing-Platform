package com.cloudvault.user.service;

import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.common.security.RoleGuard;
import com.cloudvault.domain.User;
import com.cloudvault.file.repository.FileRepository;
import com.cloudvault.user.dto.UpdateProfileRequest;
import com.cloudvault.user.dto.UserProfileResponse;
import com.cloudvault.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for user profile management and admin user operations.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleGuard roleGuard;
    private final FileRepository fileRepository;

    // ── Existing methods — unchanged ─────────────────────────────────────────

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param user the authenticated principal injected from SecurityContext
     * @return profile DTO
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User user) {
        return UserProfileResponse.from(user);
    }

    /**
     * Updates the fullName of the currently authenticated user.
     * Email and password changes require dedicated security-reviewed flows.
     *
     * @param user    the authenticated principal
     * @param request update payload
     * @return updated profile DTO
     */
    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        user.setFullName(request.getFullName());
        User saved = userRepository.save(user);
        log.info("Updated profile for user: {}", saved.getEmail());
        return UserProfileResponse.from(saved);
    }

    /**
     * Returns a paginated list of all users — admin use only.
     * The caller must already be authorised at the controller layer via @PreAuthorize.
     *
     * @param pageable pagination parameters
     * @return page of profile DTOs
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserProfileResponse::from);
    }

    /**
     * Returns a single user by their public UUID — admin use only.
     *
     * @param uuid the user's public UUID
     * @return profile DTO
     * @throws ResourceNotFoundException if no user with this UUID exists
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserByUuid(String uuid) {
        User user = findByUuidOrThrow(uuid);
        return UserProfileResponse.from(user);
    }

    /**
     * Deactivates a user account, preventing future logins.
     * Includes a programmatic RoleGuard safety check as a defence-in-depth measure.
     *
     * @param uuid      the target user's public UUID
     * @param requester the admin performing the action
     * @throws AccessDeniedException     if the requester is not an admin
     * @throws ResourceNotFoundException if the target UUID does not exist
     */
    public void deactivateUser(String uuid, User requester) {
        if (!roleGuard.isAdmin(requester)) {
            throw new AccessDeniedException("Only admins can deactivate users");
        }
        User target = findByUuidOrThrow(uuid);
        target.setIsActive(false);
        userRepository.save(target);
        log.info("Admin '{}' deactivated user '{}'", requester.getEmail(), target.getEmail());
    }

    /**
     * Reactivates a previously deactivated user account.
     *
     * @param uuid      the target user's public UUID
     * @param requester the admin performing the action
     * @throws AccessDeniedException     if the requester is not an admin
     * @throws ResourceNotFoundException if the target UUID does not exist
     */
    public void activateUser(String uuid, User requester) {
        if (!roleGuard.isAdmin(requester)) {
            throw new AccessDeniedException("Only admins can activate users");
        }
        User target = findByUuidOrThrow(uuid);
        target.setIsActive(true);
        userRepository.save(target);
        log.info("Admin '{}' activated user '{}'", requester.getEmail(), target.getEmail());
    }

    // ── New quota management methods ─────────────────────────────────────────

    /**
     * Updates the storage quota for a specific user.
     *
     * <p>Validation rules:
     * <ul>
     *   <li>{@code newQuotaBytes} must be positive (> 0).</li>
     *   <li>{@code newQuotaBytes} must be greater than the user's current
     *       {@code storageUsed} — you cannot shrink the quota below what is already
     *       consumed, as that would put the user in an indefinitely blocked state.</li>
     * </ul>
     *
     * @param userUuid      public UUID of the target user
     * @param newQuotaBytes new quota value in bytes
     * @return updated {@link UserProfileResponse}
     * @throws ResourceNotFoundException if no user with the given UUID exists
     * @throws IllegalArgumentException  if the new quota is invalid
     */
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileResponse updateStorageQuota(String userUuid, long newQuotaBytes) {
        User user = findByUuidOrThrow(userUuid);

        if (newQuotaBytes <= 0 || newQuotaBytes <= user.getStorageUsed()) {
            throw new IllegalArgumentException(
                    "New quota must be greater than current usage");
        }

        user.setStorageQuota(newQuotaBytes);
        User saved = userRepository.save(user);

        log.info("Storage quota updated for user {}: {} bytes", saved.getEmail(), newQuotaBytes);
        return UserProfileResponse.from(saved);
    }

    /**
     * Recalculates the actual storage used for a user by querying the sum of all
     * non-deleted file sizes, then updates the denormalised {@code storageUsed} counter.
     *
     * <p>This is a repair tool intended for use when the denormalised counter has
     * drifted out of sync with reality — e.g. after a bug that skipped the
     * decrement on deletion, or a manual DB operation.
     *
     * @param userUuid public UUID of the target user
     * @return updated {@link UserProfileResponse} with the corrected storage value
     * @throws ResourceNotFoundException if no user with the given UUID exists
     */
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileResponse recalculateStorageUsed(String userUuid) {
        User user = findByUuidOrThrow(userUuid);

        long previousUsed = user.getStorageUsed();
        long actualUsed   = fileRepository.sumSizeByOwnerIdAndIsDeletedFalse(user.getId());

        user.setStorageUsed(actualUsed);
        User saved = userRepository.save(user);

        log.info("Storage recalculated for user {}: actual={} bytes, previous={} bytes",
                saved.getEmail(), actualUsed, previousUsed);
        return UserProfileResponse.from(saved);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private User findByUuidOrThrow(String uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with uuid: " + uuid));
    }
}
