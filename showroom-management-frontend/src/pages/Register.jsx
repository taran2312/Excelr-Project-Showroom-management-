import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useNotify } from '../context/NotificationContext.jsx';
import { getErrorMessage } from '../services/api.js';
import './Auth.css';

export default function Register() {
  const { register, isAuthenticated } = useAuth();
  const notify = useNotify();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '', confirm: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (isAuthenticated) return <Navigate to="/dashboard" replace />;

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.password !== form.confirm) {
      setError('Passwords do not match');
      return;
    }
    setBusy(true);
    try {
      await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || null,
        password: form.password,
      });
      notify.success('Account created. Welcome aboard!');
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="card auth-card">
        <div className="card-body p-4">
          <div className="brand mb-1">
            <img src="/favicon.svg" alt="" width="32" height="32" /> Create account
          </div>
          <p className="text-muted mb-4">New accounts start with the Sales role. An admin can change it.</p>

          {error && <div className="alert alert-danger py-2">{error}</div>}

          <form onSubmit={onSubmit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="fullName">Full name</label>
              <input id="fullName" name="fullName" className="form-control" value={form.fullName}
                onChange={onChange} required maxLength={100} />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="email">Email</label>
              <input id="email" name="email" type="email" className="form-control" value={form.email}
                onChange={onChange} required autoComplete="username" />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="phone">Phone</label>
              <input id="phone" name="phone" className="form-control" value={form.phone}
                onChange={onChange} maxLength={20} />
            </div>
            <div className="row g-2 mb-3">
              <div className="col-6">
                <label className="form-label" htmlFor="password">Password</label>
                <input id="password" name="password" type="password" className="form-control" minLength={6}
                  value={form.password} onChange={onChange} required autoComplete="new-password" />
              </div>
              <div className="col-6">
                <label className="form-label" htmlFor="confirm">Confirm</label>
                <input id="confirm" name="confirm" type="password" className="form-control" minLength={6}
                  value={form.confirm} onChange={onChange} required autoComplete="new-password" />
              </div>
            </div>
            <button className="btn btn-primary w-100" type="submit" disabled={busy}>
              {busy && <span className="spinner-border spinner-border-sm me-2" />}
              Create account
            </button>
          </form>

          <div className="text-center mt-3 small">
            Already registered? <Link to="/login">Sign in</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
