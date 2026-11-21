import { useState } from 'react';
import PageHeader from '../components/PageHeader.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useNotify } from '../context/NotificationContext.jsx';
import authService from '../services/authService.js';
import { getErrorMessage } from '../services/api.js';
import { formatDate } from '../services/format.js';

export default function Profile() {
  const { user } = useAuth();
  const notify = useNotify();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.newPassword !== form.confirm) {
      setError('New passwords do not match');
      return;
    }
    setBusy(true);
    try {
      await authService.changePassword(form.currentPassword, form.newPassword);
      notify.success('Password changed');
      setForm({ currentPassword: '', newPassword: '', confirm: '' });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader title="My profile" />
      <div className="row g-3">
        <div className="col-lg-5">
          <div className="card content-card h-100">
            <div className="card-body">
              <h5 className="mb-3">{user?.fullName}</h5>
              <dl className="row mb-0 small">
                <dt className="col-4">Email</dt>
                <dd className="col-8">{user?.email}</dd>
                <dt className="col-4">Phone</dt>
                <dd className="col-8">{user?.phone || '—'}</dd>
                <dt className="col-4">Role</dt>
                <dd className="col-8"><StatusBadge status={user?.role} /></dd>
                <dt className="col-4">Showroom</dt>
                <dd className="col-8">{user?.showroomName || 'Unassigned'}</dd>
                <dt className="col-4">Member since</dt>
                <dd className="col-8">{formatDate(user?.createdAt)}</dd>
              </dl>
            </div>
          </div>
        </div>
        <div className="col-lg-7">
          <div className="card content-card">
            <div className="card-body">
              <h5 className="mb-3">Change password</h5>
              {error && <div className="alert alert-danger py-2">{error}</div>}
              <form onSubmit={onSubmit} className="row g-3">
                <div className="col-12">
                  <label className="form-label" htmlFor="currentPassword">Current password</label>
                  <input id="currentPassword" name="currentPassword" type="password" className="form-control"
                    value={form.currentPassword} onChange={onChange} required autoComplete="current-password" />
                </div>
                <div className="col-md-6">
                  <label className="form-label" htmlFor="newPassword">New password</label>
                  <input id="newPassword" name="newPassword" type="password" className="form-control" minLength={6}
                    value={form.newPassword} onChange={onChange} required autoComplete="new-password" />
                </div>
                <div className="col-md-6">
                  <label className="form-label" htmlFor="confirm">Confirm new password</label>
                  <input id="confirm" name="confirm" type="password" className="form-control" minLength={6}
                    value={form.confirm} onChange={onChange} required autoComplete="new-password" />
                </div>
                <div className="col-12 text-end">
                  <button className="btn btn-primary" type="submit" disabled={busy}>
                    {busy && <span className="spinner-border spinner-border-sm me-1" />}Update password
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
