package com.talentforge.api.access;

/** The caller is a member of the organization but their role lacks the permission. */
public class PermissionDeniedException extends RuntimeException {

    private final Permission permission;

    public PermissionDeniedException(Permission permission) {
        super("Missing permission " + permission.key());
        this.permission = permission;
    }

    public Permission getPermission() {
        return permission;
    }
}
