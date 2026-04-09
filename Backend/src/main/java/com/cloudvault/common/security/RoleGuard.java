package com.cloudvault.common.security;

import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Programmatic role and ownership checks for use inside service methods
 * where {@code @PreAuthorize} annotations are not available.
 *
 * <p>Registered as a Spring bean so it can also be referenced in SpEL expressions:
 * <pre>
 *   &#64;PreAuthorize("@roleGuard.isAdminOrOwner(#user, #fileOwnerId)")
 * </pre>
 */
@Component("roleGuard")
public class RoleGuard {

    /**
     * Returns {@code true} if the given user has the ADMIN role.
     *
     * @param user the authenticated user
     */
    public boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    /**
     * Returns {@code true} if the given user is the owner of the resource.
     *
     * @param user            the authenticated user
     * @param resourceOwnerId the internal DB id of the resource's owner
     */
    public boolean isOwner(User user, Long resourceOwnerId) {
        return user != null
                && resourceOwnerId != null
                && user.getId().equals(resourceOwnerId);
    }

    /**
     * Returns {@code true} if the user is either an admin or the resource owner.
     *
     * @param user            the authenticated user
     * @param resourceOwnerId the internal DB id of the resource's owner
     */
    public boolean isAdminOrOwner(User user, Long resourceOwnerId) {
        return isAdmin(user) || isOwner(user, resourceOwnerId);
    }

    /**
     * Extracts the currently authenticated {@link User} from the
     * {@link SecurityContextHolder}.
     *
     * @return an {@link Optional} containing the User, or empty if not authenticated
     */
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
