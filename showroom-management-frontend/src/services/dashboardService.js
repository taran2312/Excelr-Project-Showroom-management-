import api from './api.js';

const dashboardService = {
  getStats: () => api.get('/dashboard').then((r) => r.data),
};

export default dashboardService;
