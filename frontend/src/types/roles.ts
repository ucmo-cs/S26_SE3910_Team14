export type UserRole = 'ADMIN' | 'EMPLOYEE' | 'CUSTOMER';

const ROLE_STORAGE_KEY = 'demoUserRole';

export function getStoredDemoRole(): UserRole | null {
  const raw = localStorage.getItem(ROLE_STORAGE_KEY);
  if (raw === 'ADMIN' || raw === 'EMPLOYEE' || raw === 'CUSTOMER') {
    return raw;
  }
  return null;
}

export function setStoredDemoRole(role: UserRole) {
  localStorage.setItem(ROLE_STORAGE_KEY, role);
}

export function clearStoredDemoRole() {
  localStorage.removeItem(ROLE_STORAGE_KEY);
}

export function deriveRoleFromIdentity(email?: string, backendRole?: string | null): UserRole {
  if (backendRole === 'ADMIN' || backendRole === 'EMPLOYEE' || backendRole === 'CUSTOMER') {
    return backendRole;
  }
  const normalized = (email ?? '').toLowerCase();
  if (normalized.includes('admin')) {
    return 'ADMIN';
  }
  if (normalized.includes('employee') || normalized.includes('staff')) {
    return 'EMPLOYEE';
  }
  return 'CUSTOMER';
}
