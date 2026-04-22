import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getCustomerAppointments, type CustomerAppointment } from '../api/customerService';
import { useAuth } from '../context/AuthProvider';

export default function CustomerLandingPage() {
  const { user, logout } = useAuth();
  const [appointments, setAppointments] = useState<CustomerAppointment[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getCustomerAppointments()
      .then((data) => setAppointments(data))
      .finally(() => setLoading(false));
  }, []);

  return (
    <main className="min-h-screen bg-slate-100 px-4 py-8">
      <div className="mx-auto w-full max-w-5xl space-y-6">
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h1 className="text-3xl font-semibold text-blue-900">Welcome, {user?.fullName}</h1>
              <p className="mt-1 text-sm text-slate-600">{user?.email}</p>
            </div>
            <div className="flex gap-3">
              <Link
                to="/book"
                className="rounded-lg bg-blue-900 px-4 py-2 text-sm font-medium text-white hover:bg-blue-800"
              >
                Book Appointment
              </Link>
              <button
                type="button"
                onClick={logout}
                className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                Sign Out
              </button>
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold text-blue-900">Your Appointments</h2>
          {loading ? (
            <p className="mt-3 text-sm text-slate-500">Loading appointments...</p>
          ) : appointments.length === 0 ? (
            <p className="mt-3 text-sm text-slate-500">No appointments yet. Book your first one.</p>
          ) : (
            <div className="mt-4 space-y-3">
              {appointments.map((appointment) => (
                <article key={appointment.appointmentId} className="rounded-xl border border-slate-200 p-4">
                  <p className="text-sm font-medium text-blue-900">{appointment.topic}</p>
                  <p className="text-sm text-slate-700">{appointment.branch}</p>
                  <p className="text-sm text-slate-600">
                    {new Date(appointment.scheduledStart).toLocaleString()} -{' '}
                    {new Date(appointment.scheduledEnd).toLocaleTimeString()}
                  </p>
                  <p className="mt-1 text-xs uppercase tracking-wide text-slate-500">{appointment.status}</p>
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
