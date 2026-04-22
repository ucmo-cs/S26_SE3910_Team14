import axios from 'axios';
import { apiClient } from './axiosConfig';

export type DashboardAppointment = {
  id: number;
  customer: string;
  service: string;
  branch: string;
  status: string;
  scheduledAt: string;
};

export type ActivityItem = {
  id: string;
  timestamp: string;
  actionType: 'LOGIN' | 'BOOKING' | 'CANCELLATION' | 'STATUS_UPDATE';
  user: string;
  details: string;
};

export type DashboardUser = {
  id: number;
  fullName: string;
  email: string;
  role: string;
  locked: boolean;
};

export async function getRecentAppointments(): Promise<DashboardAppointment[]> {
  const { data } = await apiClient.get<DashboardAppointment[]>('/v1/dashboard/appointments/recent');
  return data;
}

export async function getSystemActivity(): Promise<ActivityItem[]> {
  const { data } = await apiClient.get<ActivityItem[]>('/v1/dashboard/activity/recent');
  return data;
}

export async function getUsersWithRoles(): Promise<DashboardUser[]> {
  const { data } = await apiClient.get<DashboardUser[]>('/v1/dashboard/users');
  return data;
}

export async function toggleUserLock(
  userId: number,
  shouldLock: boolean,
): Promise<{ simulated: boolean; success: boolean }> {
  try {
    await apiClient.patch(`/v1/dashboard/users/${userId}/lock`, { locked: shouldLock });
    return { simulated: false, success: true };
  } catch (error) {
    if (axios.isAxiosError(error)) {
      throw error;
    }
    throw error;
  }
}

export async function updateStaffAppointment(
  appointmentId: number,
  payload: { date: string; startTime: string; status?: string; notes?: string },
): Promise<DashboardAppointment> {
  const { data } = await apiClient.patch<DashboardAppointment>(
    `/v1/dashboard/appointments/${appointmentId}`,
    payload,
  );
  return data;
}

export async function deleteStaffAppointment(appointmentId: number): Promise<void> {
  await apiClient.delete(`/v1/dashboard/appointments/${appointmentId}`);
}
