import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api';

/**
 * Shared Axios instance. `withCredentials: true` ensures HttpOnly auth cookies
 * issued by the Spring Boot API are sent on same-site (or credentialed CORS) requests.
 */
export const apiClient = axios.create({
  baseURL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const url: string = error?.config?.url ?? '';
    const isAuthEndpoint = url.includes('/login') || url.includes('/register');
    if (status === 401 && !isAuthEndpoint) {
      localStorage.removeItem('customerAccessToken');
      window.location.href = '/login?reason=session-expired';
    }
    return Promise.reject(error);
  },
);
