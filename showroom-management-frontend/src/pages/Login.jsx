import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { getErrorMessage } from '../services/api.js';
import './Auth.css';

export default function Login() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (isAuthenticated) return <Navigate to="/dashboard" replace />;

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login(form.email.trim(), form.password);
      navigate(location.state?.from?.pathname || '/dashboard', { replace: true });
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
            <img src="/favicon.svg" alt="" width="32" height="32" /> Showroom Manager
          </div>
          <p className="text-muted mb-4">Sign in to manage inventory, customers and sales.</p>

          {error && <div className="alert alert-danger py-2">{error}</div>}

          <form onSubmit={onSubmit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="email">Email</label>
              <input id="email" name="email" type="email" className="form-control" autoComplete="username"
                value={form.email} onChange={onChange} required autoFocus />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="password">Password</label>
              <input id="password" name="password" type="password" className="form-control"
                autoComplete="current-password" value={form.password} onChange={onChange} required />
            </div>
            <button className="btn btn-primary w-100" type="submit" disabled={busy}>
              {busy && <span className="spinner-border spinner-border-sm me-2" />}
              Sign in
            </button>
          </form>

          <div className="text-center mt-3 small">
            New staff member? <Link to="/register">Create an account</Link>
          </div>
        </div>
      </div>
    </div>
  );
}
