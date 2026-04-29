import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthProvider';

export default function NavBar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { logout, user, role } = useAuth();
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('uiTheme') === 'dark');

  useEffect(() => {
    const root = document.documentElement;
    if (darkMode) {
      root.setAttribute('data-theme', 'dark');
      localStorage.setItem('uiTheme', 'dark');
    } else {
      root.setAttribute('data-theme', 'light');
      localStorage.setItem('uiTheme', 'light');
    }
  }, [darkMode]);

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
          <span className="text-sm font-semibold tracking-wide text-blue-900">COMMERCE BANK</span>
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
          <button
            type="button"
            onClick={() => setDarkMode((prev) => !prev)}
            className="icon-spin-hover inline-flex items-center gap-2 rounded-md border border-slate-200 px-3 py-1.5 text-xs text-slate-700 hover:bg-slate-50"
          >
            {darkMode ? (
              <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true">
                <circle cx="12" cy="12" r="5" fill="currentColor" />
                <g stroke="currentColor" strokeWidth="1.5" fill="none" strokeLinecap="round">
                  <path d="M12 2v2.5M12 19.5V22M2 12h2.5M19.5 12H22M4.9 4.9l1.8 1.8M17.3 17.3l1.8 1.8M19.1 4.9l-1.8 1.8M6.7 17.3l-1.8 1.8" />
                </g>
              </svg>
            ) : (
              <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true">
                <path
                  d="M20.2 15.1A8.5 8.5 0 1 1 8.9 3.8a7 7 0 1 0 11.3 11.3z"
                  fill="currentColor"
                />
              </svg>
            )}
            {darkMode}
          </button>
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
            className="icon-bounce-hover inline-flex items-center gap-2 rounded-md border border-slate-200 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
          >
            <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true">
              <path
                d="M10 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4M14 16l4-4-4-4M8 12h10"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.7"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
            Sign Out
          </button>
        </div>
      </div>
    </header>
  );
}
