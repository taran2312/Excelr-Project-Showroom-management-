const currency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

export const formatMoney = (value) => (value == null || value === '' ? '—' : currency.format(Number(value)));

export const formatDate = (value) => (value ? new Date(value).toLocaleDateString() : '—');

export const formatDateTime = (value) =>
  value ? new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '—';

export const titleCase = (value) =>
  value ? value.toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()) : '';

/** Converts empty strings to null so optional fields are sent as null to the API. */
export const emptyToNull = (obj) =>
  Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, v === '' ? null : v]));
