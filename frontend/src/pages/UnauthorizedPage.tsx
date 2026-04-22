import { Link } from 'react-router-dom';

export default function UnauthorizedPage() {
  return (
    <main className="min-h-screen bg-slate-100 px-4 py-10">
      <div className="mx-auto w-full max-w-xl rounded-2xl border border-amber-200 bg-white p-8 shadow-sm">
        <h1 className="text-2xl font-semibold text-amber-700">Unauthorized Access</h1>
        <p className="mt-2 text-sm text-slate-600">
          Your account is authenticated, but this section requires a different role.
        </p>
        <div className="mt-6 flex gap-3">
          <Link
            to="/dashboard"
            className="rounded-lg bg-blue-900 px-4 py-2 text-sm font-medium text-white hover:bg-blue-800"
          >
            Return to Dashboard
          </Link>
          <Link
            to="/login"
            className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Sign In Again
          </Link>
        </div>
      </div>
    </main>
  );
}
