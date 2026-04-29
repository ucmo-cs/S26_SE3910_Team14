import type { ReactElement } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import RoleProtectedRoute from './components/rbac/RoleProtectedRoute';
import BookingPage from './pages/BookingPage';
import CustomerLandingPage from './pages/CustomerLandingPage';
import EmployeeDashboardPage from './pages/EmployeeDashboardPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import UnauthorizedPage from './pages/UnauthorizedPage';
import { useAuth } from './context/AuthProvider';

function RequireAuth({ children }: { children: ReactElement }) {
  const { status } = useAuth();
  if (status === 'loading') {
    return (
      <main className="min-h-screen bg-slate-100 p-6 text-center text-sm text-slate-500">
        Loading...
      </main>
    );
  }
  if (status !== 'authenticated') {
    return <Navigate to="/login?reason=auth-required" replace />;
  }
  return children;
}

function App() {
  const { role } = useAuth();

  return (
    <Routes>
      <Route path="/" element={<Navigate to="/book" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      <Route
        path="/dashboard"
        element={
          <RequireAuth>
            {role === 'ADMIN' ? (
              <Navigate to="/admin" replace />
            ) : role === 'EMPLOYEE' ? (
              <EmployeeDashboardPage />
            ) : (
              <CustomerLandingPage />
            )}
          </RequireAuth>
        }
      />
      <Route
        path="/admin"
        element={
          <RequireAuth>
            <RoleProtectedRoute allowedRoles={['ADMIN']}>
              <AdminDashboardPage />
            </RoleProtectedRoute>
          </RequireAuth>
        }
      />
      <Route
        path="/book"
        element={<BookingPage />}
      />
      <Route path="*" element={<Navigate to="/book" replace />} />
    </Routes>
  );
}

export default App;
