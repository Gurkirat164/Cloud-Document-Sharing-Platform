package com.cloudvault.domain.enums;

// Permission level attached to a public share link: VIEW (download only), EDIT (can upload new version).
public enum SharePermission {
    VIEW,
    EDIT,
    DOWNLOAD;

    public SharePermission normalized() {
        return this == DOWNLOAD ? VIEW : this;
    }
}
