import type { ReactElement } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import BookingPage from './pages/BookingPage';
import CustomerLandingPage from './pages/CustomerLandingPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
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
    return <Navigate to="/login" replace />;
  }
  return children;
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/dashboard"
        element={
          <RequireAuth>
            <CustomerLandingPage />
          </RequireAuth>
        }
      />
      <Route
        path="/book"
        element={
          <RequireAuth>
            <BookingPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default App;
