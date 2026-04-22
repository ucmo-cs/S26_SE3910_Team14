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
