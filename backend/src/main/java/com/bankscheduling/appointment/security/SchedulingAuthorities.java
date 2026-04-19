package com.bankscheduling.appointment.security;

import java.util.Set;

/**
 * Role names that may bypass branch-scoped data access (must match {@code roles.name} in the database).
 */
public final class SchedulingAuthorities {

    public static final String SYSTEM_ADMIN = "ROLE_SYSTEM_ADMIN";
    public static final String ORG_ADMIN = "ROLE_ORG_ADMIN";

    private static final Set<String> BRANCH_SCOPE_BYPASS = Set.of(SYSTEM_ADMIN, ORG_ADMIN);

    private SchedulingAuthorities() {
    }

    public static boolean mayAccessAllBranches(String roleName) {
        return roleName != null && BRANCH_SCOPE_BYPASS.contains(roleName);
    }
}
