import { useEffect, useMemo, useState } from 'react';
import {
  getRecentAppointments,
  getSystemActivity,
  updateStaffAppointment,
  getUsersWithRoles,
  type ActivityItem,
  type DashboardAppointment,
  type DashboardUser,
} from '../api/adminService';
import ActivityFeed from '../components/admin/ActivityFeed';
import AdminDashboard from '../components/admin/AdminDashboard';
import NavBar from '../components/NavBar';

export default function AdminDashboardPage() {
  const [appointments, setAppointments] = useState<DashboardAppointment[]>([]);
  const [users, setUsers] = useState<DashboardUser[]>([]);
  const [activity, setActivity] = useState<ActivityItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshingActivity, setRefreshingActivity] = useState(false);

  useEffect(() => {
    Promise.all([getRecentAppointments(), getUsersWithRoles(), getSystemActivity()])
      .then(([appointmentData, userData, activityData]) => {
        setAppointments(appointmentData);
        setUsers(userData);
        setActivity(activityData);
      })
      .finally(() => setLoading(false));
  }, []);

  const refreshActivity = async () => {
    setRefreshingActivity(true);
    const updated = await getSystemActivity();
    setActivity(updated);
    setRefreshingActivity(false);
  };

  const changeStatus = async (
    appointmentId: number,
    status: 'APPROVED' | 'COMPLETED' | 'CANCELLED',
  ) => {
    const updated = await updateStaffAppointment(appointmentId, { status });
    setAppointments((prev) => prev.map((item) => (item.id === appointmentId ? updated : item)));
  };

  const todaysAppointments = useMemo(() => {
    const today = new Date().toISOString().slice(0, 10);
    return appointments.filter((item) => item.scheduledAt.slice(0, 10) === today).length;
  }, [appointments]);

  const failedBookingAttempts = useMemo(
    () => activity.filter((entry) => entry.actionType === 'CANCELLATION').length,
    [activity],
  );

  const activeUsers = useMemo(() => new Set(activity.map((item) => item.user)).size, [activity]);

  return (
    <main className="min-h-screen bg-slate-100">
      <NavBar />
      <div className="mx-auto w-full max-w-6xl space-y-6 px-4 py-8">
        {loading ? (
          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="h-5 w-56 animate-pulse rounded bg-slate-200" />
            <div className="mt-3 h-4 w-80 animate-pulse rounded bg-slate-200" />
            <div className="mt-6 grid gap-4 md:grid-cols-3">
              <div className="h-24 animate-pulse rounded-xl bg-slate-100" />
              <div className="h-24 animate-pulse rounded-xl bg-slate-100" />
              <div className="h-24 animate-pulse rounded-xl bg-slate-100" />
            </div>
          </section>
        ) : (
          <>
            <AdminDashboard
              appointments={appointments}
              users={users}
              onUsersUpdated={setUsers}
              onStatusChange={changeStatus}
              todaysAppointments={todaysAppointments}
              failedBookingAttempts={failedBookingAttempts}
              activeUsers={activeUsers}
            />
            <ActivityFeed items={activity} onRefresh={refreshActivity} refreshing={refreshingActivity} />
          </>
        )}
      </div>
    </main>
  );
}
