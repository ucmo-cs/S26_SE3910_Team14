import { useEffect, useState } from 'react';
import { getCustomerAppointments, type CustomerAppointment } from '../api/customerService';
import AppointmentStatusTimeline from '../components/appointments/AppointmentStatusTimeline';
import NavBar from '../components/NavBar';
import { getFriendlyErrorMessage } from '../utils/httpError';

export default function EmployeeDashboardPage() {
  const [appointments, setAppointments] = useState<CustomerAppointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getCustomerAppointments()
      .then((data) => setAppointments(data))
      .catch((err) =>
        setError(getFriendlyErrorMessage(err, 'Unable to load appointment queue right now.')),
      )
      .finally(() => setLoading(false));
  }, []);

  return (
    <main className="min-h-screen bg-slate-100">
      <NavBar />
      <div className="mx-auto w-full max-w-5xl space-y-6 px-4 py-8">
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h1 className="text-3xl font-semibold text-blue-900">Employee Appointment Queue</h1>
          <p className="mt-1 text-sm text-slate-600">
            Review customer bookings and monitor status progression.
          </p>
        </section>
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          {loading ? (
            <p className="text-sm text-slate-500">Loading appointment queue...</p>
          ) : error ? (
            <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          ) : appointments.length === 0 ? (
            <p className="text-sm text-slate-500">No queued appointments at this moment.</p>
          ) : (
            <div className="space-y-4">
              {appointments.map((appointment) => (
                <article key={appointment.appointmentId} className="rounded-xl border border-slate-200 p-4">
                  <div className="flex flex-wrap justify-between gap-2">
                    <div>
                      <p className="font-medium text-blue-900">{appointment.topic}</p>
                      <p className="text-sm text-slate-600">{appointment.branch}</p>
                    </div>
                    <p className="text-sm text-slate-600">
                      {new Date(appointment.scheduledStart).toLocaleString()}
                    </p>
                  </div>
                  <AppointmentStatusTimeline status={appointment.status} />
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
