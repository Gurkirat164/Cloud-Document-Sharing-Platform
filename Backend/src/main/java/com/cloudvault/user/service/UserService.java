package com.cloudvault.user.service;

import com.cloudvault.common.exception.ResourceNotFoundException;
import com.cloudvault.common.security.RoleGuard;
import com.cloudvault.domain.User;
import com.cloudvault.user.dto.UpdateProfileRequest;
import com.cloudvault.user.dto.UserProfileResponse;
import com.cloudvault.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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

    // ===== Private helpers =====

    private User findByUuidOrThrow(String uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with uuid: " + uuid));
    }
}
