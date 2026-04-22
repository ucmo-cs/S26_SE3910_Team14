import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import AppointmentStatusTimeline from '../components/appointments/AppointmentStatusTimeline';
import NavBar from '../components/NavBar';
import {
  deleteCustomerAppointment,
  getCustomerAppointments,
  updateCustomerAppointment,
  type CustomerAppointment,
} from '../api/customerService';
import { useAuth } from '../context/AuthProvider';
import { getFriendlyErrorMessage } from '../utils/httpError';

export default function CustomerLandingPage() {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState<CustomerAppointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editDate, setEditDate] = useState('');
  const [editTime, setEditTime] = useState('09:00');

  useEffect(() => {
    getCustomerAppointments()
      .then((data) => setAppointments(data))
      .catch((err) =>
        setError(getFriendlyErrorMessage(err, 'Unable to load appointments. Please try again soon.')),
      )
      .finally(() => setLoading(false));
  }, []);

  const handleDelete = async (appointmentId: number) => {
    try {
      await deleteCustomerAppointment(appointmentId);
      setAppointments((prev) => prev.filter((item) => item.appointmentId !== appointmentId));
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Unable to delete this appointment right now.'));
    }
  };

  const handleSave = async (appointmentId: number) => {
    try {
      const updated = await updateCustomerAppointment(appointmentId, { date: editDate, startTime: editTime });
      setAppointments((prev) => prev.map((item) => (item.appointmentId === appointmentId ? updated : item)));
      setEditingId(null);
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Unable to update this appointment right now.'));
    }
  };

  return (
    <main className="min-h-screen bg-slate-100">
      <NavBar />
      <div className="mx-auto w-full max-w-5xl space-y-6 px-4 py-8">
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
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold text-blue-900">Your Appointments</h2>
          {loading ? (
            <div className="mt-4 space-y-3">
              <div className="h-16 animate-pulse rounded-xl bg-slate-100" />
              <div className="h-16 animate-pulse rounded-xl bg-slate-100" />
            </div>
          ) : error ? (
            <div className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
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
                  <AppointmentStatusTimeline status={appointment.status} />
                  {editingId === appointment.appointmentId ? (
                    <div className="mt-3 flex flex-wrap items-center gap-2">
                      <input
                        type="date"
                        value={editDate}
                        onChange={(event) => setEditDate(event.target.value)}
                        className="rounded-md border border-slate-200 px-2 py-1 text-sm"
                      />
                      <input
                        type="time"
                        value={editTime}
                        onChange={(event) => setEditTime(event.target.value)}
                        className="rounded-md border border-slate-200 px-2 py-1 text-sm"
                      />
                      <button
                        type="button"
                        onClick={() => handleSave(appointment.appointmentId)}
                        className="rounded-md bg-blue-900 px-3 py-1.5 text-xs font-medium text-white"
                      >
                        Save
                      </button>
                      <button
                        type="button"
                        onClick={() => setEditingId(null)}
                        className="rounded-md border border-slate-200 px-3 py-1.5 text-xs text-slate-700"
                      >
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <div className="mt-3 flex gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          setEditingId(appointment.appointmentId);
                          setEditDate(appointment.scheduledStart.slice(0, 10));
                          setEditTime(new Date(appointment.scheduledStart).toTimeString().slice(0, 5));
                        }}
                        className="rounded-md border border-slate-200 px-3 py-1.5 text-xs text-slate-700 hover:bg-slate-50"
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDelete(appointment.appointmentId)}
                        className="rounded-md border border-red-200 px-3 py-1.5 text-xs text-red-700 hover:bg-red-50"
                      >
                        Delete
                      </button>
                    </div>
                  )}
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
