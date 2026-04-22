import axios from 'axios';

export function getFriendlyErrorMessage(error: unknown, fallback: string): string {
  if (!axios.isAxiosError(error)) {
    return fallback;
  }

  const status = error.response?.status;
  if (status === 401) {
    return 'Your session has expired. Please sign in again.';
  }
  if (status === 403) {
    return 'You do not have permission to perform this action.';
  }
  if (status === 409) {
    return 'That time slot is no longer available. Please choose another appointment time.';
  }

  const serverMessage = error.response?.data?.message;
  if (typeof serverMessage === 'string' && serverMessage.trim()) {
    return serverMessage;
  }
  return fallback;
}
