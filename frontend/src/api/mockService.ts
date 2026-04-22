import type { Branch, CustomerDetails, Topic } from '../context/BookingContext';

export type AppointmentPayload = {
  topic: Topic;
  branch: Branch;
  date: string;
  time: string;
  customerDetails: CustomerDetails;
};

const topics: Topic[] = [
  {
    id: 'general-banking',
    name: 'General Banking',
    description: 'Everyday account support, wire transfers, and account updates.',
  },
  {
    id: 'loans',
    name: 'Loans',
    description: 'Home, auto, and personal lending consultations.',
  },
  {
    id: 'credit-cards',
    name: 'Credit Cards',
    description: 'New cards, limit review, and rewards guidance.',
  },
];

const branchesByTopic: Record<string, Branch[]> = {
  'general-banking': [
    {
      id: 'downtown',
      name: 'Downtown Financial Center',
      addressLine1: '120 Market Street',
      addressLine2: 'Milwaukee, WI 53202',
    },
    {
      id: 'oak-creek',
      name: 'Oak Creek Branch',
      addressLine1: '8701 S Howell Ave',
      addressLine2: 'Oak Creek, WI 53154',
    },
    {
      id: 'shorewood',
      name: 'Shorewood Banking Office',
      addressLine1: '4020 N Oakland Ave',
      addressLine2: 'Shorewood, WI 53211',
    },
  ],
  loans: [
    {
      id: 'mortgage-hub',
      name: 'Mortgage Advisory Hub',
      addressLine1: '230 E Buffalo St',
      addressLine2: 'Milwaukee, WI 53202',
    },
    {
      id: 'brookfield',
      name: 'Brookfield Lending Center',
      addressLine1: '16200 W Bluemound Rd',
      addressLine2: 'Brookfield, WI 53005',
    },
  ],
  'credit-cards': [
    {
      id: 'west-allis',
      name: 'West Allis Credit Office',
      addressLine1: '7010 W Greenfield Ave',
      addressLine2: 'West Allis, WI 53214',
    },
    {
      id: 'bay-view',
      name: 'Bay View Banking Center',
      addressLine1: '2500 S Kinnickinnic Ave',
      addressLine2: 'Milwaukee, WI 53207',
    },
  ],
};

function simulateNetwork<T>(data: T, delayMs = 450): Promise<T> {
  return new Promise((resolve) => {
    setTimeout(() => resolve(data), delayMs);
  });
}

function formatTime(hour24: number, minute: number): string {
  const suffix = hour24 >= 12 ? 'PM' : 'AM';
  const hour12 = hour24 % 12 === 0 ? 12 : hour24 % 12;
  const minutes = minute.toString().padStart(2, '0');
  return `${hour12.toString().padStart(2, '0')}:${minutes} ${suffix}`;
}

export async function getTopics(): Promise<Topic[]> {
  return simulateNetwork(topics);
}

export async function getBranches(topicId: string): Promise<Branch[]> {
  const branches = branchesByTopic[topicId] ?? [];
  return simulateNetwork(branches);
}

export async function getAvailableTimeslots(
  branchId: string,
  date: string,
): Promise<string[]> {
  const seed = `${branchId}-${date}`.length;
  const slots: string[] = [];

  for (let hour = 9; hour < 17; hour += 1) {
    slots.push(formatTime(hour, 0));
    slots.push(formatTime(hour, 30));
  }

  const filtered = slots.filter((_, index) => (index + seed) % 5 !== 0);
  return simulateNetwork(filtered);
}

export async function submitAppointment(data: AppointmentPayload): Promise<{
  confirmationNumber: string;
  submittedAt: string;
}> {
  return simulateNetwork({
    confirmationNumber: `BNK-${Date.now().toString().slice(-8)}`,
    submittedAt: new Date().toISOString(),
    ...data,
  });
}
