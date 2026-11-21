import api, { crud } from './api.js';

const vehicleService = {
  ...crud('/vehicles'),
  updateStatus: (id, status) => api.patch(`/vehicles/${id}/status`, { status }).then((r) => r.data),
};

export default vehicleService;
