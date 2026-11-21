import { useCallback, useEffect, useState } from 'react';
import PageHeader from '../components/PageHeader.jsx';
import Modal from '../components/Modal.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import Loader from '../components/Loader.jsx';
import EmptyState from '../components/EmptyState.jsx';
import FormField from '../components/FormField.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useNotify } from '../context/NotificationContext.jsx';
import customerService from '../services/customerService.js';
import { getErrorMessage } from '../services/api.js';
import { emptyToNull, formatDate } from '../services/format.js';

const emptyForm = { fullName: '', email: '', phone: '', address: '', city: '', notes: '' };

export default function Customers() {
  const { hasRole } = useAuth();
  const notify = useNotify();
  const canDelete = hasRole('ADMIN', 'MANAGER');

  const [customers, setCustomers] = useState([]);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [toDelete, setToDelete] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setCustomers(await customerService.list({ q: q.trim() || null }));
    } catch (err) {
      notify.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [q, notify]);

  useEffect(() => {
    const t = setTimeout(load, 250);
    return () => clearTimeout(t);
  }, [load]);

  const openNew = () => {
    setForm(emptyForm);
    setFormError('');
    setEditing({});
  };

  const openEdit = (c) => {
    setForm(Object.fromEntries(Object.keys(emptyForm).map((k) => [k, c[k] ?? ''])));
    setFormError('');
    setEditing(c);
  };

  const close = () => setEditing(null);
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    try {
      if (editing.id) {
        await customerService.update(editing.id, emptyToNull(form));
        notify.success('Customer updated');
      } else {
        await customerService.create(emptyToNull(form));
        notify.success('Customer added');
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
      await customerService.remove(toDelete.id);
      notify.success('Customer deleted');
      setToDelete(null);
      load();
    } catch (err) {
      notify.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHeader title="Customers" subtitle="Leads and buyers">
        <button className="btn btn-primary" onClick={openNew}>+ Add customer</button>
      </PageHeader>

      <div className="card content-card mb-3">
        <div className="card-body">
          <input className="form-control" placeholder="Search name, email or phone…" value={q}
            onChange={(e) => setQ(e.target.value)} />
        </div>
      </div>

      <div className="card content-card">
        {loading ? <Loader /> : customers.length === 0 ? <EmptyState message="No customers found." /> : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Contact</th>
                  <th>City</th>
                  <th>Notes</th>
                  <th>Added</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {customers.map((c) => (
                  <tr key={c.id}>
                    <td className="fw-semibold">{c.fullName}</td>
                    <td>
                      <div>{c.phone}</div>
                      <div className="small text-muted">{c.email}</div>
                    </td>
                    <td>{c.city || '—'}</td>
                    <td className="small text-muted text-truncate" style={{ maxWidth: 240 }}>{c.notes}</td>
                    <td className="small">{formatDate(c.createdAt)}</td>
                    <td className="text-end text-nowrap">
                      <button className="btn btn-sm btn-outline-primary me-1" onClick={() => openEdit(c)}>Edit</button>
                      {canDelete && (
                        <button className="btn btn-sm btn-outline-danger" onClick={() => setToDelete(c)}>Delete</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal show={editing !== null} title={editing?.id ? 'Edit customer' : 'Add customer'} onClose={close}>
        <form onSubmit={onSubmit}>
          {formError && <div className="alert alert-danger py-2">{formError}</div>}
          <div className="row g-3">
            <FormField label="Full name" name="fullName" value={form.fullName} onChange={onChange} required col="col-12" maxLength={100} />
            <FormField label="Phone" name="phone" value={form.phone} onChange={onChange} required maxLength={20} />
            <FormField label="Email" name="email" type="email" value={form.email} onChange={onChange} />
            <FormField label="Address" name="address" value={form.address} onChange={onChange} col="col-md-8" />
            <FormField label="City" name="city" value={form.city} onChange={onChange} col="col-md-4" />
            <FormField label="Notes" name="notes" as="textarea" value={form.notes} onChange={onChange} col="col-12"
              placeholder="Interested models, budget, follow-up…" maxLength={1000} />
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
        message={toDelete && `Delete customer ${toDelete.fullName}?`}
        busy={saving}
        onConfirm={onDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
