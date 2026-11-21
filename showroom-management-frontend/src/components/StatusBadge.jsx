import { titleCase } from '../services/format.js';

const VARIANTS = {
  AVAILABLE: 'success',
  RESERVED: 'warning',
  SOLD: 'secondary',
  IN_SERVICE: 'info',
  PENDING: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'danger',
  SCHEDULED: 'primary',
  NO_SHOW: 'dark',
  ADMIN: 'danger',
  MANAGER: 'primary',
  SALES: 'info',
};

export default function StatusBadge({ status }) {
  if (!status) return null;
  return <span className={`badge rounded-pill text-bg-${VARIANTS[status] || 'light'}`}>{titleCase(status)}</span>;
}
