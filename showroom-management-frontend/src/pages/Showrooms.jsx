import { useCallback, useEffect, useState } from 'react';
import PageHeader from '../components/PageHeader.jsx';
import Modal from '../components/Modal.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import Loader from '../components/Loader.jsx';
import EmptyState from '../components/EmptyState.jsx';
import FormField from '../components/FormField.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useNotify } from '../context/NotificationContext.jsx';
import showroomService from '../services/showroomService.js';
import { getErrorMessage } from '../services/api.js';
import { emptyToNull } from '../services/format.js';
import './Showrooms.css';

const emptyForm = { name: '', code: '', address: '', city: '', phone: '', email: '' };

export default function Showrooms() {
  const { hasRole } = useAuth();
  const notify = useNotify();
  const isAdmin = hasRole('ADMIN');

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
      setShowrooms(await showroomService.list());
    } catch (err) {
      notify.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [notify]);

  useEffect(() => {
    load();
  }, [load]);

  const openNew = () => {
    setForm(emptyForm);
    setFormError('');
    setEditing({});
  };

  const openEdit = (s) => {
    setForm(Object.fromEntries(Object.keys(emptyForm).map((k) => [k, s[k] ?? ''])));
    setFormError('');
    setEditing(s);
  };

  const close = () => setEditing(null);
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    try {
      if (editing.id) {
        await showroomService.update(editing.id, emptyToNull(form));
        notify.success('Showroom updated');
      } else {
        await showroomService.create(emptyToNull(form));
        notify.success('Showroom created');
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
      await showroomService.remove(toDelete.id);
      notify.success('Showroom deleted');
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
      <PageHeader title="Showrooms" subtitle="Branches and locations">
        {isAdmin && <button className="btn btn-primary" onClick={openNew}>+ Add showroom</button>}
      </PageHeader>

      {loading ? <Loader /> : showrooms.length === 0 ? <EmptyState message="No showrooms yet." /> : (
        <div className="row g-3">
          {showrooms.map((s) => (
            <div className="col-md-6 col-xl-4" key={s.id}>
              <div className="card showroom-card h-100">
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <div>
                      <h5 className="mb-0">{s.name}</h5>
                      <span className="badge text-bg-light font-monospace">{s.code}</span>
                    </div>
                    <div className="text-end">
                      <div className="showroom-count">{s.vehicleCount}</div>
                      <div className="small text-muted">vehicles</div>
                    </div>
                  </div>
                  <div className="small text-muted">
                    {[s.address, s.city].filter(Boolean).join(', ') || 'No address'}
                  </div>
                  <div className="small mt-2">
                    {s.phone && <div>📞 {s.phone}</div>}
                    {s.email && <div>✉️ {s.email}</div>}
                  </div>
                </div>
                {isAdmin && (
                  <div className="card-footer bg-transparent d-flex gap-2 justify-content-end">
                    <button className="btn btn-sm btn-outline-primary" onClick={() => openEdit(s)}>Edit</button>
                    <button className="btn btn-sm btn-outline-danger" onClick={() => setToDelete(s)}>Delete</button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal show={editing !== null} title={editing?.id ? 'Edit showroom' : 'Add showroom'} onClose={close}>
        <form onSubmit={onSubmit}>
          {formError && <div className="alert alert-danger py-2">{formError}</div>}
          <div className="row g-3">
            <FormField label="Name" name="name" value={form.name} onChange={onChange} required col="col-md-8" maxLength={100} />
            <FormField label="Code" name="code" value={form.code} onChange={onChange} required col="col-md-4" maxLength={20}
              placeholder="e.g. PUN-02" />
            <FormField label="Address" name="address" value={form.address} onChange={onChange} col="col-md-8" />
            <FormField label="City" name="city" value={form.city} onChange={onChange} col="col-md-4" />
            <FormField label="Phone" name="phone" value={form.phone} onChange={onChange} maxLength={20} />
            <FormField label="Email" name="email" type="email" value={form.email} onChange={onChange} />
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
        message={toDelete && `Delete showroom ${toDelete.name}? It must have no vehicles or staff.`}
        busy={saving}
        onConfirm={onDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
