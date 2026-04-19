import { createContext, useContext, useMemo, type ReactNode } from 'react';

export type AuthStatus = 'unknown' | 'authenticated' | 'unauthenticated';

export type AuthContextValue = {
  status: AuthStatus;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/**
 * Skeleton for global auth state and refresh-token handling.
 * Next steps: attach a response interceptor on `apiClient` for 401 → POST /auth/refresh
 * (cookies only; never read tokens from JSON), then reconcile `status`.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const value = useMemo<AuthContextValue>(
    () => ({
      status: 'unknown',
    }),
    [],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}
