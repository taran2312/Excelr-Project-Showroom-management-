import api, { crud } from './api.js';

const saleService = {
  ...crud('/sales'),
  updateStatus: (id, status) => api.patch(`/sales/${id}/status`, { status }).then((r) => r.data),
};

export default saleService;
