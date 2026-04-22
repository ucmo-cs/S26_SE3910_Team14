import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthProvider';

export default function NavBar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { logout, user, role } = useAuth();

  const navItems =
    role === 'ADMIN'
      ? [
          { to: '/admin', label: 'Admin Dashboard' },
          { to: '/dashboard', label: 'Home' },
        ]
      : role === 'EMPLOYEE'
        ? [{ to: '/dashboard', label: 'Appointment Queue' }]
        : [
            { to: '/dashboard', label: 'Dashboard' },
            { to: '/book', label: 'Book Appointment' },
          ];

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex w-full max-w-5xl items-center justify-between px-4 py-3">
        <div className="flex items-center gap-4">
          <span className="text-sm font-semibold tracking-wide text-blue-900">CENTRAL BANK</span>
          <nav className="flex gap-2">
            {navItems.map((item) => {
              const isActive = location.pathname === item.to;
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={`rounded-md px-3 py-1.5 text-sm transition ${
                    isActive
                      ? 'bg-blue-900 text-white'
                      : 'text-slate-700 hover:bg-slate-100 hover:text-blue-900'
                  }`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>
        <div className="flex items-center gap-3">
          <span className="hidden rounded-full border border-slate-200 px-2 py-1 text-xs font-medium text-slate-700 md:inline">
            {role}
          </span>
          <span className="hidden text-sm text-slate-600 md:inline">{user?.email}</span>
          <button
            type="button"
            onClick={() => {
              logout();
              navigate('/login', { replace: true });
            }}
            className="rounded-md border border-slate-200 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
          >
            Sign Out
          </button>
        </div>
      </div>
    </header>
  );
}
