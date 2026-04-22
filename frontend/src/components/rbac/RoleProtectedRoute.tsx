import type { ReactElement } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthProvider';
import type { UserRole } from '../../types/roles';

type RoleProtectedRouteProps = {
  children: ReactElement;
  allowedRoles: UserRole[];
};

export default function RoleProtectedRoute({ children, allowedRoles }: RoleProtectedRouteProps) {
  const { role } = useAuth();
  const location = useLocation();

  if (!allowedRoles.includes(role)) {
    return <Navigate to="/unauthorized" replace state={{ from: location.pathname }} />;
  }
  return children;
}
