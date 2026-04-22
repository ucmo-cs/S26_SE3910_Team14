import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { apiClient } from '../api/axiosConfig';
import { deriveRoleFromIdentity, type UserRole } from '../types/roles';

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';

export type CustomerUser = {
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  role?: string;
};

export type AuthContextValue = {
  status: AuthStatus;
  user: CustomerUser | null;
  role: UserRole;
  login: (email: string, password: string) => Promise<void>;
  register: (firstName: string, lastName: string, email: string, password: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);
const AUTH_TOKEN_KEY = 'customerAccessToken';

function setAuthHeader(token: string | null) {
  if (token) {
    apiClient.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    delete apiClient.defaults.headers.common.Authorization;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [user, setUser] = useState<CustomerUser | null>(null);
  const [role, setRole] = useState<UserRole>('CUSTOMER');

  useEffect(() => {
    const token = localStorage.getItem(AUTH_TOKEN_KEY);
    if (!token) {
      setStatus('unauthenticated');
      return;
    }

    setAuthHeader(token);
    apiClient
      .get<CustomerUser>('/v1/customer/me')
      .then((response) => {
        const resolvedRole = deriveRoleFromIdentity(response.data.email, response.data.role);
        setUser(response.data);
        setRole(resolvedRole);
        setStatus('authenticated');
      })
      .catch(() => {
        localStorage.removeItem(AUTH_TOKEN_KEY);
        setAuthHeader(null);
        setUser(null);
        setRole('CUSTOMER');
        setStatus('unauthenticated');
      });
  }, []);

  const login = async (email: string, password: string) => {
    const response = await apiClient.post<{ token: string; customer: CustomerUser }>(
      '/v1/public/auth/customer/login',
      { email, password },
    );
    const resolvedRole = deriveRoleFromIdentity(response.data.customer.email, response.data.customer.role);
    localStorage.setItem(AUTH_TOKEN_KEY, response.data.token);
    setAuthHeader(response.data.token);
    setUser(response.data.customer);
    setRole(resolvedRole);
    setStatus('authenticated');
  };

  const register = async (
    firstName: string,
    lastName: string,
    email: string,
    password: string,
  ) => {
    const response = await apiClient.post<{ token: string; customer: CustomerUser }>(
      '/v1/public/auth/customer/register',
      { firstName, lastName, email, password },
    );
    const resolvedRole = deriveRoleFromIdentity(response.data.customer.email, response.data.customer.role);
    localStorage.setItem(AUTH_TOKEN_KEY, response.data.token);
    setAuthHeader(response.data.token);
    setUser(response.data.customer);
    setRole(resolvedRole);
    setStatus('authenticated');
  };

  const logout = () => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    setAuthHeader(null);
    setUser(null);
    setRole('CUSTOMER');
    setStatus('unauthenticated');
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      role,
      login,
      register,
      logout,
    }),
    [status, user, role],
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
