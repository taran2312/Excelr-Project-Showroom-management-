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
import testDriveService from '../services/testDriveService.js';
import vehicleService from '../services/vehicleService.js';
import customerService from '../services/customerService.js';
import { getErrorMessage } from '../services/api.js';
import { emptyToNull, formatDateTime, titleCase } from '../services/format.js';

const STATUSES = ['SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];

/** "YYYY-MM-DDTHH:mm" in local time, for <input type="datetime-local">. */
const toLocalInput = (date) => {
  const d = new Date(date);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
};

const emptyForm = () => {
  const inAnHour = new Date(Date.now() + 60 * 60 * 1000);
  inAnHour.setMinutes(0, 0, 0);
  return { vehicleId: '', customerId: '', scheduledAt: toLocalInput(inAnHour), feedback: '' };
};

export default function TestDrives() {
  const { hasRole } = useAuth();
  const notify = useNotify();
  const canDelete = hasRole('ADMIN', 'MANAGER');

  const [drives, setDrives] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [vehicles, setVehicles] = useState([]);
  const [customers, setCustomers] = useState([]);

  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  const [closing, setClosing] = useState(null); // {drive, status}
  const [closeNote, setCloseNote] = useState('');
  const [toDelete, setToDelete] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setDrives(await testDriveService.list({ status: statusFilter || null }));
    } catch (err) {
      notify.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [statusFilter, notify]);

  useEffect(() => {
    load();
  }, [load]);

  const loadLookups = async () => {
    const [available, reserved, custs] = await Promise.all([
      vehicleService.list({ status: 'AVAILABLE' }),
      vehicleService.list({ status: 'RESERVED' }),
      customerService.list(),
    ]);
    setVehicles([...available, ...reserved]);
    setCustomers(custs);
  };

  const openNew = async () => {
    setForm(emptyForm());
    setFormError('');
    setEditing({});
    loadLookups().catch((err) => setFormError(getErrorMessage(err)));
  };

  const openEdit = async (d) => {
    setForm({
      vehicleId: d.vehicleId, customerId: d.customerId,
      scheduledAt: d.scheduledAt?.slice(0, 16) ?? '', feedback: d.feedback ?? '',
    });
    setFormError('');
    setEditing(d);
    loadLookups().catch((err) => setFormError(getErrorMessage(err)));
  };

  const close = () => setEditing(null);
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    const payload = emptyToNull({
      ...form,
      vehicleId: Number(form.vehicleId),
      customerId: Number(form.customerId),
    });
    try {
      if (editing.id) {
        await testDriveService.update(editing.id, payload);
        notify.success('Test drive updated');
      } else {
        await testDriveService.create(payload);
        notify.success('Test drive scheduled');
      }
      close();
      load();
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const submitClose = async () => {
    setSaving(true);
    try {
      await testDriveService.updateStatus(closing.drive.id, closing.status, closeNote);
      notify.success(`Marked as ${titleCase(closing.status)}`);
      setClosing(null);
      load();
    } catch (err) {
      notify.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const onDelete = async () => {
    setSaving(true);
    try {
      await testDriveService.remove(toDelete.id);
      notify.success('Test drive deleted');
      setToDelete(null);
      load();
    } catch (err) {
      notify.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const isScheduled = !editing?.id || editing.status === 'SCHEDULED';
  // Keep the drive's current vehicle selectable even if it is no longer available.
  const vehicleOptions = editing?.id && !vehicles.some((v) => v.id === editing.vehicleId)
    ? [{ id: editing.vehicleId, label: editing.vehicleLabel }, ...vehicles]
    : vehicles;

  return (
    <>
      <PageHeader title="Test Drives" subtitle="Schedule and track customer test drives">
        <select className="form-select w-auto" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{titleCase(s)}</option>)}
        </select>
        <button className="btn btn-primary" onClick={openNew}>+ Schedule</button>
      </PageHeader>

      <div className="card content-card">
        {loading ? <Loader /> : drives.length === 0 ? <EmptyState message="No test drives found." /> : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>When</th>
                  <th>Vehicle</th>
                  <th>Customer</th>
                  <th>Staff</th>
                  <th>Feedback</th>
                  <th>Status</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {drives.map((d) => (
                  <tr key={d.id}>
                    <td className="text-nowrap">{formatDateTime(d.scheduledAt)}</td>
                    <td className="small">{d.vehicleLabel}</td>
                    <td>{d.customerName}</td>
                    <td>{d.salespersonName || '—'}</td>
                    <td className="small text-muted text-truncate" style={{ maxWidth: 220 }}>{d.feedback}</td>
                    <td><StatusBadge status={d.status} /></td>
                    <td className="text-end text-nowrap">
                      <button className="btn btn-sm btn-outline-primary me-1" onClick={() => openEdit(d)}>Edit</button>
                      {d.status === 'SCHEDULED' && (
                        <div className="btn-group me-1">
                          <button type="button" className="btn btn-sm btn-outline-success dropdown-toggle"
                            data-bs-toggle="dropdown" aria-expanded="false"
                            data-bs-popper-config='{"strategy":"fixed"}'>
                            Close
                          </button>
                          <ul className="dropdown-menu dropdown-menu-end">
                            {['COMPLETED', 'NO_SHOW', 'CANCELLED'].map((st) => (
                              <li key={st}>
                                <button className="dropdown-item" type="button"
                                  onClick={() => { setCloseNote(d.feedback || ''); setClosing({ drive: d, status: st }); }}>
                                  {titleCase(st)}
                                </button>
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}
                      {canDelete && (
                        <button className="btn btn-sm btn-outline-danger" onClick={() => setToDelete(d)}>Delete</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal show={editing !== null} title={editing?.id ? 'Edit test drive' : 'Schedule test drive'} onClose={close}>
        <form onSubmit={onSubmit}>
          {formError && <div className="alert alert-danger py-2">{formError}</div>}
          {!isScheduled && (
            <div className="alert alert-info py-2 small">This test drive is closed; only feedback can be edited.</div>
          )}
          <div className="row g-3">
            <FormField label="Vehicle" name="vehicleId" as="select" value={form.vehicleId} onChange={onChange}
              required disabled={!isScheduled} col="col-12">
              <option value="">Select…</option>
              {vehicleOptions.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.label || `${v.year} ${v.make} ${v.model} ${v.variant || ''} — ${v.vin}`}
                </option>
              ))}
            </FormField>
            <FormField label="Customer" name="customerId" as="select" value={form.customerId} onChange={onChange}
              required disabled={!isScheduled}>
              <option value="">Select…</option>
              {customers.map((c) => <option key={c.id} value={c.id}>{c.fullName} ({c.phone})</option>)}
            </FormField>
            <FormField label="Date & time" name="scheduledAt" type="datetime-local" value={form.scheduledAt}
              onChange={onChange} required disabled={!isScheduled} />
            <FormField label="Feedback / notes" name="feedback" as="textarea" value={form.feedback} onChange={onChange}
              col="col-12" maxLength={1000} />
          </div>
          <div className="d-flex justify-content-end gap-2 mt-4">
            <button type="button" className="btn btn-light" onClick={close}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving && <span className="spinner-border spinner-border-sm me-1" />}Save
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        show={closing !== null}
        title={closing ? `Mark as ${titleCase(closing.status)}` : ''}
        onClose={() => setClosing(null)}
        footer={
          <>
            <button className="btn btn-light" onClick={() => setClosing(null)}>Cancel</button>
            <button className="btn btn-primary" onClick={submitClose} disabled={saving}>Confirm</button>
          </>
        }
      >
        <label className="form-label" htmlFor="closeNote">Feedback (optional)</label>
        <textarea id="closeNote" className="form-control" rows={3} value={closeNote} maxLength={1000}
          onChange={(e) => setCloseNote(e.target.value)} placeholder="How did it go? Next steps?" />
      </Modal>

      <ConfirmDialog
        show={toDelete !== null}
        message={toDelete && `Delete the test drive for ${toDelete.customerName}?`}
        busy={saving}
        onConfirm={onDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
