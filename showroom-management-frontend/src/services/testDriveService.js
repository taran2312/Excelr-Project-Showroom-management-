import api, { crud } from './api.js';

const testDriveService = {
  ...crud('/test-drives'),
  updateStatus: (id, status, note) =>
    api.patch(`/test-drives/${id}/status`, { status, note }).then((r) => r.data),
};

export default testDriveService;
