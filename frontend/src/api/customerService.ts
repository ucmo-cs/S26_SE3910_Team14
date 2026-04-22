import { apiClient } from './axiosConfig';

export type CustomerAppointment = {
  appointmentId: number;
  topic: string;
  branch: string;
  scheduledStart: string;
  scheduledEnd: string;
  status: string;
};

export async function getCustomerAppointments(status?: string): Promise<CustomerAppointment[]> {
  const response = await apiClient.get<CustomerAppointment[]>('/v1/customer/appointments', {
    params: status && status !== 'ALL' ? { status } : undefined,
  });
  return response.data;
}

export async function updateCustomerAppointment(
  appointmentId: number,
  payload: { date: string; startTime: string; notes?: string },
): Promise<CustomerAppointment> {
  const response = await apiClient.put<CustomerAppointment>(
    `/v1/customer/appointments/${appointmentId}`,
    payload,
  );
  return response.data;
}

export async function cancelCustomerAppointment(appointmentId: number): Promise<void> {
  await apiClient.patch(`/v1/customer/appointments/${appointmentId}/cancel`);
}
