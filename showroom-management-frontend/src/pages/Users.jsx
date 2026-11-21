import { useCallback, useEffect, useState } from 'react';
import PageHeader from '../components/PageHeader.jsx';
import Modal from '../components/Modal.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import Loader from '../components/Loader.jsx';
import EmptyState from '../components/EmptyState.jsx';
import FormField from '../components/FormField.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useNotify } from '../context/NotificationContext.jsx';
import userService from '../services/userService.js';
import showroomService from '../services/showroomService.js';
import { getErrorMessage } from '../services/api.js';
import { emptyToNull, formatDate, titleCase } from '../services/format.js';

const ROLES = ['ADMIN', 'MANAGER', 'SALES'];
const emptyForm = { fullName: '', email: '', phone: '', password: '', role: 'SALES', showroomId: '', active: true };

export default function Users() {
  const { hasRole, user: me } = useAuth();
  const notify = useNotify();
  const isAdmin = hasRole('ADMIN');

  const [users, setUsers] = useState([]);
  const [showrooms, setShowrooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [toDelete, setToDelete] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setUsers(await userService.list());
    } catch (err) {
      notify.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => {
    load();
    showroomService.list().then(setShowrooms).catch(() => {});
  }, [load]);

  const openNew = () => {
    setForm(emptyForm);
    setFormError('');
    setEditing({});
  };

  const openEdit = (u) => {
    setForm({
      fullName: u.fullName, email: u.email, phone: u.phone ?? '', password: '',
      role: u.role, showroomId: u.showroomId ?? '', active: u.active,
    });
    setFormError('');
    setEditing(u);
  };

  const close = () => setEditing(null);
  const onChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    const payload = emptyToNull({
      ...form,
      showroomId: form.showroomId === '' ? '' : Number(form.showroomId),
    });
    try {
      if (editing.id) {
        await userService.update(editing.id, payload);
        notify.success('Staff member updated');
      } else {
        await userService.create(payload);
        notify.success('Staff member created');
      }
      close();
      load();
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const onDelete = async () => {
    setSaving(true);
    try {
      await userService.remove(toDelete.id);
      notify.success('Staff member deleted');
      setToDelete(null);
      load();
    } catch (err) {
      notify.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const editingSelf = editing?.id === me?.id;

  return (
    <>
      <PageHeader title="Staff" subtitle={isAdmin ? 'Accounts, roles and showroom assignment' : 'Team directory (read-only)'}>
        {isAdmin && <button className="btn btn-primary" onClick={openNew}>+ Add staff</button>}
      </PageHeader>

      <div className="card content-card">
        {loading ? <Loader /> : users.length === 0 ? <EmptyState /> : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Showroom</th>
                  <th>Status</th>
                  <th>Joined</th>
                  {isAdmin && <th className="text-end">Actions</th>}
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id} className={u.active ? '' : 'text-muted'}>
                    <td className="fw-semibold">
                      {u.fullName} {u.id === me?.id && <span className="badge text-bg-light">you</span>}
                      <div className="small text-muted fw-normal">{u.phone}</div>
                    </td>
                    <td>{u.email}</td>
                    <td><StatusBadge status={u.role} /></td>
                    <td>{u.showroomName || '—'}</td>
                    <td>
                      {u.active
                        ? <span className="badge text-bg-success">Active</span>
                        : <span className="badge text-bg-secondary">Inactive</span>}
                    </td>
                    <td className="small">{formatDate(u.createdAt)}</td>
                    {isAdmin && (
                      <td className="text-end text-nowrap">
                        <button className="btn btn-sm btn-outline-primary me-1" onClick={() => openEdit(u)}>Edit</button>
                        {u.id !== me?.id && (
                          <button className="btn btn-sm btn-outline-danger" onClick={() => setToDelete(u)}>Delete</button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal show={editing !== null} title={editing?.id ? 'Edit staff member' : 'Add staff member'} onClose={close}>
        <form onSubmit={onSubmit}>
          {formError && <div className="alert alert-danger py-2">{formError}</div>}
          <div className="row g-3">
            <FormField label="Full name" name="fullName" value={form.fullName} onChange={onChange} required col="col-12" />
            <FormField label="Email" name="email" type="email" value={form.email} onChange={onChange} required />
            <FormField label="Phone" name="phone" value={form.phone} onChange={onChange} maxLength={20} />
            <FormField label={editing?.id ? 'New password (leave blank to keep)' : 'Password'} name="password"
              type="password" value={form.password} onChange={onChange} required={!editing?.id} minLength={6}
              autoComplete="new-password" col="col-12" />
            <FormField label="Role" name="role" as="select" value={form.role} onChange={onChange} required disabled={editingSelf}>
              {ROLES.map((r) => <option key={r} value={r}>{titleCase(r)}</option>)}
            </FormField>
            <FormField label="Showroom" name="showroomId" as="select" value={form.showroomId} onChange={onChange}>
              <option value="">Unassigned</option>
              {showrooms.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </FormField>
            <div className="col-12">
              <div className="form-check form-switch">
                <input className="form-check-input" type="checkbox" id="active" name="active" checked={form.active}
                  onChange={onChange} disabled={editingSelf} />
                <label className="form-check-label" htmlFor="active">Account active</label>
              </div>
            </div>
          </div>
          <div className="d-flex justify-content-end gap-2 mt-4">
            <button type="button" className="btn btn-light" onClick={close}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving && <span className="spinner-border spinner-border-sm me-1" />}Save
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        show={toDelete !== null}
        message={toDelete && `Delete ${toDelete.fullName}? Staff with sales history should be deactivated instead.`}
        busy={saving}
        onConfirm={onDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
