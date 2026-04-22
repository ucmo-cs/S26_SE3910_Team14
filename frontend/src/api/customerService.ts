import { apiClient } from './axiosConfig';

export type CustomerAppointment = {
  appointmentId: number;
  topic: string;
  branch: string;
  scheduledStart: string;
  scheduledEnd: string;
  status: string;
};

export async function getCustomerAppointments(): Promise<CustomerAppointment[]> {
  const response = await apiClient.get<CustomerAppointment[]>('/v1/customer/appointments');
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

export async function deleteCustomerAppointment(appointmentId: number): Promise<void> {
  await apiClient.delete(`/v1/customer/appointments/${appointmentId}`);
}
