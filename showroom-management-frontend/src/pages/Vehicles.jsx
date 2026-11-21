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
import vehicleService from '../services/vehicleService.js';
import showroomService from '../services/showroomService.js';
import { getErrorMessage } from '../services/api.js';
import { emptyToNull, formatMoney, titleCase } from '../services/format.js';

const STATUSES = ['AVAILABLE', 'RESERVED', 'SOLD', 'IN_SERVICE'];
const FUELS = ['Petrol', 'Diesel', 'Hybrid', 'Electric', 'CNG'];
const TRANSMISSIONS = ['Manual', 'Automatic'];

const emptyForm = {
  vin: '', make: '', model: '', variant: '', year: new Date().getFullYear(), color: '',
  fuelType: '', transmission: '', price: '', mileage: 0, status: 'AVAILABLE', showroomId: '', description: '',
};

export default function Vehicles() {
  const { hasRole } = useAuth();
  const notify = useNotify();
  const canEdit = hasRole('ADMIN', 'MANAGER');

  const [vehicles, setVehicles] = useState([]);
  const [showrooms, setShowrooms] = useState([]);
  const [filters, setFilters] = useState({ q: '', status: '', showroomId: '' });
  const [loading, setLoading] = useState(true);

  const [editing, setEditing] = useState(null); // null = closed, {} = new, vehicle = edit
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [toDelete, setToDelete] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setVehicles(await vehicleService.list(emptyToNull(filters)));
    } catch (err) {
      notify.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [filters, notify]);

  useEffect(() => {
    const t = setTimeout(load, 250); // debounce search typing
    return () => clearTimeout(t);
  }, [load]);

  useEffect(() => {
    showroomService.list().then(setShowrooms).catch(() => {});
  }, []);

  const openNew = () => {
    setForm({ ...emptyForm, showroomId: showrooms[0]?.id ?? '' });
    setFormError('');
    setEditing({});
  };

  const openEdit = (v) => {
    setForm({ ...emptyForm, ...Object.fromEntries(Object.entries(v).map(([k, val]) => [k, val ?? ''])) });
    setFormError('');
    setEditing(v);
  };

  const close = () => setEditing(null);
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    const payload = emptyToNull({
      vin: form.vin, make: form.make, model: form.model, variant: form.variant, year: Number(form.year),
      color: form.color, fuelType: form.fuelType, transmission: form.transmission, price: form.price,
      mileage: form.mileage === '' ? null : Number(form.mileage), status: form.status,
      showroomId: form.showroomId === '' ? '' : Number(form.showroomId), description: form.description,
    });
    try {
      if (editing.id) {
        await vehicleService.update(editing.id, payload);
        notify.success('Vehicle updated');
      } else {
        await vehicleService.create(payload);
        notify.success('Vehicle added to inventory');
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
      await vehicleService.remove(toDelete.id);
      notify.success('Vehicle deleted');
      setToDelete(null);
      load();
    } catch (err) {
      notify.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  // Sale-controlled statuses cannot be set by hand.
  const statusLocked = editing?.id && ['RESERVED', 'SOLD'].includes(editing.status);

  return (
    <>
      <PageHeader title="Vehicles" subtitle="Showroom inventory">
        {canEdit && (
          <button className="btn btn-primary" onClick={openNew} disabled={showrooms.length === 0}
            title={showrooms.length === 0 ? 'Create a showroom first' : undefined}>
            + Add vehicle
          </button>
        )}
      </PageHeader>

      <div className="card content-card mb-3">
        <div className="card-body row g-2">
          <div className="col-md-5">
            <input className="form-control" placeholder="Search make, model or VIN…" value={filters.q}
              onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
          </div>
          <div className="col-6 col-md-3">
            <select className="form-select" value={filters.status}
              onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
              <option value="">All statuses</option>
              {STATUSES.map((s) => <option key={s} value={s}>{titleCase(s)}</option>)}
            </select>
          </div>
          <div className="col-6 col-md-4">
            <select className="form-select" value={filters.showroomId}
              onChange={(e) => setFilters({ ...filters, showroomId: e.target.value })}>
              <option value="">All showrooms</option>
              {showrooms.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
        </div>
      </div>

      <div className="card content-card">
        {loading ? <Loader /> : vehicles.length === 0 ? <EmptyState message="No vehicles match." /> : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>Vehicle</th>
                  <th>VIN</th>
                  <th>Details</th>
                  <th>Showroom</th>
                  <th className="text-end">Price</th>
                  <th>Status</th>
                  {canEdit && <th className="text-end">Actions</th>}
                </tr>
              </thead>
              <tbody>
                {vehicles.map((v) => (
                  <tr key={v.id}>
                    <td>
                      <div className="fw-semibold">{v.year} {v.make} {v.model}</div>
                      <div className="small text-muted">{v.variant}</div>
                    </td>
                    <td className="font-monospace small">{v.vin}</td>
                    <td className="small text-muted">
                      {[v.color, v.fuelType, v.transmission].filter(Boolean).join(' · ')}
                      {v.mileage != null && <div>{v.mileage.toLocaleString()} km</div>}
                    </td>
                    <td>{v.showroomName}</td>
                    <td className="text-end">{formatMoney(v.price)}</td>
                    <td><StatusBadge status={v.status} /></td>
                    {canEdit && (
                      <td className="text-end text-nowrap">
                        <button className="btn btn-sm btn-outline-primary me-1" onClick={() => openEdit(v)}>Edit</button>
                        <button className="btn btn-sm btn-outline-danger" onClick={() => setToDelete(v)}>Delete</button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal show={editing !== null} title={editing?.id ? 'Edit vehicle' : 'Add vehicle'} onClose={close} size="lg">
        <form onSubmit={onSubmit}>
          {formError && <div className="alert alert-danger py-2">{formError}</div>}
          <div className="row g-3">
            <FormField label="VIN" name="vin" value={form.vin} onChange={onChange} required minLength={5} maxLength={17} />
            <FormField label="Showroom" name="showroomId" as="select" value={form.showroomId} onChange={onChange} required>
              <option value="">Select…</option>
              {showrooms.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </FormField>
            <FormField label="Make" name="make" value={form.make} onChange={onChange} required col="col-md-4" />
            <FormField label="Model" name="model" value={form.model} onChange={onChange} required col="col-md-4" />
            <FormField label="Variant" name="variant" value={form.variant} onChange={onChange} col="col-md-4" />
            <FormField label="Year" name="year" type="number" min={1950} max={2100} value={form.year} onChange={onChange} required col="col-md-4" />
            <FormField label="Color" name="color" value={form.color} onChange={onChange} col="col-md-4" />
            <FormField label="Mileage (km)" name="mileage" type="number" min={0} value={form.mileage} onChange={onChange} col="col-md-4" />
            <FormField label="Fuel" name="fuelType" as="select" value={form.fuelType} onChange={onChange} col="col-md-4">
              <option value="">—</option>
              {FUELS.map((f) => <option key={f}>{f}</option>)}
            </FormField>
            <FormField label="Transmission" name="transmission" as="select" value={form.transmission} onChange={onChange} col="col-md-4">
              <option value="">—</option>
              {TRANSMISSIONS.map((t) => <option key={t}>{t}</option>)}
            </FormField>
            <FormField label="Price (₹)" name="price" type="number" min={0} step="0.01" value={form.price} onChange={onChange} required col="col-md-4" />
            <FormField label="Status" name="status" as="select" value={form.status} onChange={onChange} disabled={statusLocked}>
              {(statusLocked ? STATUSES : ['AVAILABLE', 'IN_SERVICE']).map((s) => (
                <option key={s} value={s}>{titleCase(s)}</option>
              ))}
            </FormField>
            {statusLocked && (
              <div className="col-md-6 small text-muted d-flex align-items-end">Status is controlled by this vehicle's sale.</div>
            )}
            <FormField label="Description" name="description" as="textarea" value={form.description} onChange={onChange} col="col-12" maxLength={1000} />
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
        message={toDelete && `Delete ${toDelete.year} ${toDelete.make} ${toDelete.model} (${toDelete.vin})?`}
        busy={saving}
        onConfirm={onDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  );
}
