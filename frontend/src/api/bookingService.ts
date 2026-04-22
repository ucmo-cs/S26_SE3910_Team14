import { apiClient } from './axiosConfig';
import type { Branch, CustomerDetails, Topic } from '../context/BookingContext';

const bookingBasePath = '/v1/public/booking';

type TopicApiResponse = {
  id: number;
  code: string;
  displayName: string;
  description: string;
  defaultDurationMinutes: number;
};

type BranchApiResponse = {
  id: number;
  displayName: string;
  streetLine1: string;
  streetLine2: string | null;
  city: string | null;
  stateProvince: string | null;
  postalCode: string | null;
  timeZone: string;
};

type TimeslotApiResponse = {
  timeZone: string;
  slotDurationMinutes: number;
  slots: string[];
};

export type SubmitAppointmentPayload = {
  topic: Topic;
  branch: Branch;
  date: string;
  time: string;
  customerDetails: CustomerDetails;
};

export async function getTopics(): Promise<Topic[]> {
  const { data } = await apiClient.get<TopicApiResponse[]>(`${bookingBasePath}/topics`);
  return data.map((topic) => ({
    id: topic.id,
    code: topic.code,
    name: topic.displayName,
    description: topic.description,
    defaultDurationMinutes: topic.defaultDurationMinutes,
  }));
}

export async function getBranches(topicId?: number): Promise<Branch[]> {
  const { data } = await apiClient.get<BranchApiResponse[]>(`${bookingBasePath}/branches`, {
    params: topicId ? { topicId } : undefined,
  });
  return data;
}

export async function getAvailableTimeslots(
  branchId: number,
  topicId: number,
  date: string,
): Promise<TimeslotApiResponse> {
  const { data } = await apiClient.get<TimeslotApiResponse>(`${bookingBasePath}/times`, {
    params: { branchId, topicId, date },
  });
  return data;
}

export async function submitAppointment(data: SubmitAppointmentPayload): Promise<{
  appointmentId: number;
  message: string;
}> {
  const payload = {
    firstName: data.customerDetails.firstName,
    lastName: data.customerDetails.lastName,
    email: data.customerDetails.email,
    branchId: data.branch.id,
    serviceTypeId: data.topic.id,
    date: data.date,
    startTime: data.time,
  };

  const response = await apiClient.post<{ appointmentId: number; message: string }>(
    `${bookingBasePath}/book`,
    payload,
  );
  return response.data;
}
