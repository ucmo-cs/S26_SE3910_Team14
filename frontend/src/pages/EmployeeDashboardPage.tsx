import { useEffect, useState } from 'react';
import { getRecentAppointments, updateStaffAppointment, type DashboardAppointment } from '../api/adminService';
import AppointmentStatusTimeline from '../components/appointments/AppointmentStatusTimeline';
import NavBar from '../components/NavBar';
import { getStatusCardClass, getStatusPillClass } from '../utils/appointmentStatus';
import { getFriendlyErrorMessage } from '../utils/httpError';

export default function EmployeeDashboardPage() {
  const [appointments, setAppointments] = useState<DashboardAppointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editDate, setEditDate] = useState('');
  const [editTime, setEditTime] = useState('09:00');
  const [editStatus, setEditStatus] = useState('REQUESTED');

  useEffect(() => {
    getRecentAppointments()
      .then((data) => setAppointments(data))
      .catch((err) =>
        setError(getFriendlyErrorMessage(err, 'Unable to load appointment queue right now.')),
      )
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async (appointmentId: number) => {
    try {
      const updated = await updateStaffAppointment(appointmentId, {
        date: editDate,
        startTime: editTime,
        status: editStatus,
      });
      setAppointments((prev) => prev.map((item) => (item.id === appointmentId ? updated : item)));
      setEditingId(null);
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Unable to update appointment right now.'));
    }
  };

  const setStatus = async (appointmentId: number, status: 'APPROVED' | 'COMPLETED' | 'CANCELLED') => {
    try {
      const current = appointments.find((item) => item.id === appointmentId);
      if (!current) return;
      const updated = await updateStaffAppointment(appointmentId, {
        date: current.scheduledAt.slice(0, 10),
        startTime: new Date(current.scheduledAt).toTimeString().slice(0, 5),
        status,
      });
      setAppointments((prev) => prev.map((item) => (item.id === appointmentId ? updated : item)));
    } catch (err) {
      setError(getFriendlyErrorMessage(err, 'Unable to update appointment status right now.'));
    }
  };

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
                <article
                  key={appointment.id}
                  className={`rounded-xl border p-4 ${getStatusCardClass(appointment.status)}`}
                >
                  <div className="flex flex-wrap justify-between gap-2">
                    <div>
                      <p className="font-medium text-blue-900">{appointment.service}</p>
                      <p className="text-sm text-slate-700">{appointment.customer}</p>
                      <p className="text-sm text-slate-600">{appointment.branch}</p>
                    </div>
                    <p className="text-sm text-slate-600">
                      {new Date(appointment.scheduledAt).toLocaleString()}
                    </p>
                  </div>
                  <AppointmentStatusTimeline status={appointment.status} />
                  <span
                    className={`mt-2 inline-flex rounded-full border px-2 py-0.5 text-xs font-medium ${getStatusPillClass(appointment.status)}`}
                  >
                    {appointment.status}
                  </span>
                  {editingId === appointment.id ? (
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
                      <select
                        value={editStatus}
                        onChange={(event) => setEditStatus(event.target.value)}
                        className="rounded-md border border-slate-200 px-2 py-1 text-sm"
                      >
                        <option value="REQUESTED">Requested</option>
                        <option value="APPROVED">Approved</option>
                        <option value="COMPLETED">Completed</option>
                        <option value="CANCELLED">Cancelled</option>
                      </select>
                      <button
                        type="button"
                        onClick={() => handleSave(appointment.id)}
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
                          setEditingId(appointment.id);
                          setEditDate(appointment.scheduledAt.slice(0, 10));
                          setEditTime(new Date(appointment.scheduledAt).toTimeString().slice(0, 5));
                          setEditStatus(appointment.status);
                        }}
                        className="rounded-md border border-slate-200 px-3 py-1.5 text-xs text-slate-700 hover:bg-slate-50"
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => setStatus(appointment.id, 'APPROVED')}
                        disabled={appointment.status !== 'REQUESTED'}
                        className="rounded-md border border-emerald-200 px-3 py-1.5 text-xs text-emerald-700 hover:bg-emerald-50 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        Approve
                      </button>
                      <button
                        type="button"
                        onClick={() => setStatus(appointment.id, 'COMPLETED')}
                        disabled={appointment.status !== 'APPROVED'}
                        className="rounded-md border border-emerald-800 px-3 py-1.5 text-xs text-emerald-900 hover:bg-emerald-100 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        Complete
                      </button>
                      <button
                        type="button"
                        onClick={() => setStatus(appointment.id, 'CANCELLED')}
                        disabled={appointment.status === 'CANCELLED' || appointment.status === 'COMPLETED'}
                        className="rounded-md border border-red-200 px-3 py-1.5 text-xs text-red-700 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        Cancel
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
