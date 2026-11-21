import axios from 'axios';

export const TOKEN_KEY = 'showroom_token';
export const USER_KEY = 'showroom_user';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

// Attach the JWT to every request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Let the auth context react to expired/invalid sessions.
let unauthorizedHandler = null;
export function setUnauthorizedHandler(fn) {
  unauthorizedHandler = fn;
}

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || '';
    if (status === 401 && !url.includes('/auth/login') && unauthorizedHandler) {
      unauthorizedHandler();
    }
    return Promise.reject(error);
  },
);

/** Turns an Axios error into a user-facing message (+ field errors from the backend). */
export function getErrorMessage(error) {
  const data = error?.response?.data;
  if (data?.fieldErrors) {
    return Object.entries(data.fieldErrors)
      .map(([field, msg]) => `${field}: ${msg}`)
      .join(' • ');
  }
  if (data?.message) return data.message;
  // ERR_NETWORK: direct call failed. Bare 5xx without a JSON body: the Vite dev proxy could not reach the backend.
  if (error?.code === 'ERR_NETWORK' || (error?.response?.status >= 500 && !data)) {
    return 'Cannot reach the server. Is the backend running?';
  }
  return error?.message || 'Something went wrong';
}

/** Small CRUD helper shared by the resource services. */
export function crud(path) {
  return {
    list: (params) => api.get(path, { params }).then((r) => r.data),
    get: (id) => api.get(`${path}/${id}`).then((r) => r.data),
    create: (body) => api.post(path, body).then((r) => r.data),
    update: (id, body) => api.put(`${path}/${id}`, body).then((r) => r.data),
    remove: (id) => api.delete(`${path}/${id}`),
  };
}

export default api;
