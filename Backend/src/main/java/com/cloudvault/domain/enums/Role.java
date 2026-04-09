package com.cloudvault.domain.enums;

import org.springframework.security.core.GrantedAuthority;

/**
 * User role controlled at DB level via ENUM('USER','ADMIN').
 *
 * <p>Implements GrantedAuthority directly so Role instances can be used
 * without any String mapping at the call site. The authority string
 * follows Spring Security's ROLE_ prefix convention.
 */
public enum Role implements GrantedAuthority {

    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    /** Returns the Spring Security authority string, e.g. "ROLE_ADMIN". */
    @Override
    public String getAuthority() {
        return authority;
    }
}
