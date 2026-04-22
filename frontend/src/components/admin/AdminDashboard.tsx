import { useMemo, useState } from 'react';
import type { DashboardAppointment, DashboardUser } from '../../api/adminService';
import { toggleUserLock } from '../../api/adminService';
import { getStatusPillClass } from '../../utils/appointmentStatus';

type AdminDashboardProps = {
  appointments: DashboardAppointment[];
  users: DashboardUser[];
  onUsersUpdated: (users: DashboardUser[]) => void;
  onStatusChange: (
    appointmentId: number,
    status: 'APPROVED' | 'COMPLETED' | 'CANCELLED',
  ) => Promise<void>;
  activeUsers: number;
  todaysAppointments: number;
  failedBookingAttempts: number;
};

export default function AdminDashboard({
  appointments,
  users,
  onUsersUpdated,
  onStatusChange,
  activeUsers,
  todaysAppointments,
  failedBookingAttempts,
}: AdminDashboardProps) {
  const [userFilter, setUserFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [dateFilter, setDateFilter] = useState('');
  const [actionMessage, setActionMessage] = useState('');

  const filteredAppointments = useMemo(() => {
    return appointments.filter((appointment) => {
      const matchesUser =
        !userFilter.trim() || appointment.customer.toLowerCase().includes(userFilter.toLowerCase());
      const matchesStatus = statusFilter === 'ALL' || appointment.status === statusFilter;
      const matchesDate =
        !dateFilter || appointment.scheduledAt.slice(0, 10) === dateFilter.trim().slice(0, 10);
      return matchesUser && matchesStatus && matchesDate;
    });
  }, [appointments, userFilter, statusFilter, dateFilter]);

  const handleToggleLock = async (targetUser: DashboardUser) => {
    const updated = users.map((item) =>
      item.id === targetUser.id ? { ...item, locked: !item.locked } : item,
    );
    onUsersUpdated(updated);
    const result = await toggleUserLock(targetUser.id, !targetUser.locked);
    setActionMessage(
      result.simulated
        ? `User lock simulated for ${targetUser.fullName} (demo mode).`
        : `User ${targetUser.fullName} updated successfully.`,
    );
  };

  return (
    <section className="space-y-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-2xl font-semibold text-blue-900">Admin Dashboard</h2>
          <p className="text-sm text-slate-600">
            Visibility into appointments, user access, and system health.
          </p>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="Appointments Today" value={String(todaysAppointments)} />
        <MetricCard label="Active Users (Approx.)" value={String(activeUsers)} />
        <MetricCard label="Failed Booking Attempts" value={String(failedBookingAttempts)} />
      </div>

      <div className="grid gap-3 md:grid-cols-3">
        <input
          value={userFilter}
          onChange={(event) => setUserFilter(event.target.value)}
          placeholder="Filter by customer"
          className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
        />
        <input
          value={dateFilter}
          onChange={(event) => setDateFilter(event.target.value)}
          type="date"
          className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
        />
        <select
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
          className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
        >
          <option value="ALL">All statuses</option>
          <option value="REQUESTED">Requested</option>
          <option value="APPROVED">Approved</option>
          <option value="COMPLETED">Completed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full min-w-[700px] border-collapse text-left text-sm">
          <thead>
            <tr className="border-b border-slate-200 text-slate-600">
              <th className="py-2 pr-4 font-medium">Appointment</th>
              <th className="py-2 pr-4 font-medium">Customer</th>
              <th className="py-2 pr-4 font-medium">Service</th>
              <th className="py-2 pr-4 font-medium">Status</th>
              <th className="py-2 pr-4 font-medium">Actions</th>
              <th className="py-2 font-medium">Scheduled</th>
            </tr>
          </thead>
          <tbody>
            {filteredAppointments.length === 0 ? (
              <tr>
                <td className="py-4 text-slate-500" colSpan={6}>
                  No appointments match your current filters.
                </td>
              </tr>
            ) : (
              filteredAppointments.map((item) => (
                <tr key={item.id} className="border-b border-slate-100">
                  <td className="py-2 pr-4 font-medium text-slate-700">#{item.id}</td>
                  <td className="py-2 pr-4 text-slate-700">{item.customer}</td>
                  <td className="py-2 pr-4 text-slate-700">{item.service}</td>
                  <td className="py-2 pr-4">
                    <span
                      className={`rounded-full border px-2 py-0.5 text-xs ${getStatusPillClass(item.status)}`}
                    >
                      {item.status}
                    </span>
                  </td>
                  <td className="py-2 pr-4">
                    <div className="flex flex-wrap gap-1">
                      <button
                        type="button"
                        onClick={() => onStatusChange(item.id, 'APPROVED')}
                        disabled={item.status !== 'REQUESTED'}
                        className="rounded border border-emerald-200 px-2 py-1 text-xs text-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        Approve
                      </button>
                      <button
                        type="button"
                        onClick={() => onStatusChange(item.id, 'COMPLETED')}
                        disabled={item.status !== 'APPROVED'}
                        className="rounded border border-emerald-800 px-2 py-1 text-xs text-emerald-900 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        Complete
                      </button>
                      <button
                        type="button"
                        onClick={() => onStatusChange(item.id, 'CANCELLED')}
                        disabled={item.status === 'CANCELLED' || item.status === 'COMPLETED'}
                        className="rounded border border-red-200 px-2 py-1 text-xs text-red-700 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        Cancel
                      </button>
                    </div>
                  </td>
                  <td className="py-2 text-slate-600">{new Date(item.scheduledAt).toLocaleString()}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div>
        <h3 className="text-lg font-semibold text-blue-900">User Roles & Account Control</h3>
        <div className="mt-3 overflow-x-auto">
          <table className="w-full min-w-[640px] border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-600">
                <th className="py-2 pr-4 font-medium">User</th>
                <th className="py-2 pr-4 font-medium">Email</th>
                <th className="py-2 pr-4 font-medium">Role</th>
                <th className="py-2 font-medium">Account Status</th>
              </tr>
            </thead>
            <tbody>
              {users.map((item) => (
                <tr key={item.id} className="border-b border-slate-100">
                  <td className="py-2 pr-4 font-medium text-slate-700">{item.fullName}</td>
                  <td className="py-2 pr-4 text-slate-600">{item.email}</td>
                  <td className="py-2 pr-4">
                    <span className="rounded-full border border-slate-200 px-2 py-0.5 text-xs text-slate-700">
                      {item.role}
                    </span>
                  </td>
                  <td className="py-2">
                    <button
                      type="button"
                      onClick={() => handleToggleLock(item)}
                      className={`rounded-lg px-3 py-1.5 text-xs font-medium ${
                        item.locked
                          ? 'bg-emerald-100 text-emerald-700 hover:bg-emerald-200'
                          : 'bg-amber-100 text-amber-800 hover:bg-amber-200'
                      }`}
                    >
                      {item.locked ? 'Unlock User' : 'Lock User'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {actionMessage ? (
          <p className="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
            {actionMessage}
          </p>
        ) : null}
      </div>
    </section>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <article className="rounded-xl border border-slate-200 bg-slate-50 p-4">
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold text-blue-900">{value}</p>
    </article>
  );
}
