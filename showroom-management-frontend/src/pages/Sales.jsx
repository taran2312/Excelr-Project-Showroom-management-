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
import saleService from '../services/saleService.js';
import vehicleService from '../services/vehicleService.js';
import customerService from '../services/customerService.js';
import userService from '../services/userService.js';
import { getErrorMessage } from '../services/api.js';
import { emptyToNull, formatDate, formatMoney, titleCase } from '../services/format.js';

const STATUSES = ['PENDING', 'COMPLETED', 'CANCELLED'];
const PAYMENT_METHODS = ['CASH', 'CARD', 'BANK_TRANSFER', 'FINANCE'];
const today = () => new Date().toISOString().slice(0, 10);

const emptyForm = {
  vehicleId: '', customerId: '', salespersonId: '', salePrice: '', discount: 0,
  paymentMethod: 'CASH', saleDate: today(), notes: '',
};

export default function Sales() {
  const { hasRole } = useAuth();
  const notify = useNotify();
  const isManager = hasRole('ADMIN', 'MANAGER');
  const isAdmin = hasRole('ADMIN');

  const [sales, setSales] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);

  const [vehicles, setVehicles] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [staff, setStaff] = useState([]);

  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [pendingAction, setPendingAction] = useState(null); // {sale, type: 'COMPLETED'|'CANCELLED'|'DELETE'}

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setSales(await saleService.list({ status: statusFilter || null }));
    } catch (err) {
      notify.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [statusFilter, notify]);

  useEffect(() => {
    load();
  }, [load]);

  const loadLookups = async (currentVehicle) => {
    const [avail, custs, users] = await Promise.all([
      vehicleService.list({ status: 'AVAILABLE' }),
      customerService.list(),
      isManager ? userService.list() : Promise.resolve([]),
    ]);
    // When editing, keep the sale's own (reserved) vehicle in the list.
    setVehicles(currentVehicle ? [currentVehicle, ...avail] : avail);
    setCustomers(custs);
    setStaff(users.filter((u) => u.active));
  };

  const openNew = async () => {
    setForm({ ...emptyForm, saleDate: today() });
    setFormError('');
    setEditing({});
    try {
      await loadLookups(null);
    } catch (err) {
      setFormError(getErrorMessage(err));
    }
  };

  const openEdit = async (s) => {
    setForm({
      vehicleId: s.vehicleId, customerId: s.customerId, salespersonId: s.salespersonId ?? '',
      salePrice: s.salePrice, discount: s.discount ?? 0, paymentMethod: s.paymentMethod,
      saleDate: s.saleDate, notes: s.notes ?? '',
    });
    setFormError('');
    setEditing(s);
    try {
      await loadLookups({ id: s.vehicleId, label: s.vehicleLabel });
    } catch (err) {
      setFormError(getErrorMessage(err));
    }
  };

  const close = () => setEditing(null);

  const onChange = (e) => {
    const { name, value } = e.target;
    const next = { ...form, [name]: value };
    // Pre-fill the sale price from the vehicle's list price.
    if (name === 'vehicleId' && !editing?.id) {
      const v = vehicles.find((x) => String(x.id) === value);
      if (v?.price != null) next.salePrice = v.price;
    }
    setForm(next);
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    const payload = emptyToNull({
      ...form,
      vehicleId: Number(form.vehicleId),
      customerId: Number(form.customerId),
      salespersonId: form.salespersonId === '' ? '' : Number(form.salespersonId),
      discount: form.discount === '' ? 0 : form.discount,
    });
    try {
      if (editing.id) {
        await saleService.update(editing.id, payload);
        notify.success('Sale updated');
      } else {
        await saleService.create(payload);
        notify.success('Sale recorded — vehicle reserved');
      }
      close();
      load();
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const runAction = async () => {
    const { sale, type } = pendingAction;
    setSaving(true);
    try {
      if (type === 'DELETE') {
        await saleService.remove(sale.id);
        notify.success('Sale deleted');
      } else {
        await saleService.updateStatus(sale.id, type);
        notify.success(type === 'COMPLETED' ? 'Sale completed — vehicle marked sold' : 'Sale cancelled — vehicle released');
      }
      setPendingAction(null);
      load();
    } catch (err) {
      notify.error(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const finalAmount = Number(form.salePrice || 0) - Number(form.discount || 0);

  const actionText = {
    COMPLETED: { title: 'Complete sale', label: 'Complete', variant: 'success', msg: 'The vehicle will be marked SOLD. This cannot be undone.' },
    CANCELLED: { title: 'Cancel sale', label: 'Cancel sale', variant: 'warning', msg: 'The vehicle will be released back to AVAILABLE.' },
    DELETE: { title: 'Delete sale', label: 'Delete', variant: 'danger', msg: 'This permanently removes the sale record.' },
  }[pendingAction?.type] || {};

  return (
    <>
      <PageHeader title="Sales" subtitle="Bookings, approvals and invoices">
        <select className="form-select w-auto" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{titleCase(s)}</option>)}
        </select>
        <button className="btn btn-primary" onClick={openNew}>+ New sale</button>
      </PageHeader>

      <div className="card content-card">
        {loading ? <Loader /> : sales.length === 0 ? <EmptyState message="No sales found." /> : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>Invoice</th>
                  <th>Vehicle</th>
                  <th>Customer</th>
                  <th>Salesperson</th>
                  <th className="text-end">Amount</th>
                  <th>Payment</th>
                  <th>Date</th>
                  <th>Status</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {sales.map((s) => (
                  <tr key={s.id}>
                    <td className="font-monospace small">{s.invoiceNumber}</td>
                    <td className="small">{s.vehicleLabel}</td>
                    <td>{s.customerName}</td>
                    <td>{s.salespersonName}</td>
                    <td className="text-end">
                      {formatMoney(s.finalAmount)}
                      {Number(s.discount) > 0 && <div className="small text-muted">−{formatMoney(s.discount)}</div>}
                    </td>
                    <td className="small">{titleCase(s.paymentMethod)}</td>
                    <td className="small">{formatDate(s.saleDate)}</td>
                    <td><StatusBadge status={s.status} /></td>
                    <td className="text-end text-nowrap">
                      {s.status === 'PENDING' && (
                        <>
                          <button className="btn btn-sm btn-outline-primary me-1" onClick={() => openEdit(s)}>Edit</button>
                          {isManager && (
                            <>
                              <button className="btn btn-sm btn-success me-1"
                                onClick={() => setPendingAction({ sale: s, type: 'COMPLETED' })}>Complete</button>
                              <button className="btn btn-sm btn-outline-warning me-1"
                                onClick={() => setPendingAction({ sale: s, type: 'CANCELLED' })}>Cancel</button>
                            </>
                          )}
                        </>
                      )}
                      {isAdmin && s.status !== 'COMPLETED' && (
                        <button className="btn btn-sm btn-outline-danger"
                          onClick={() => setPendingAction({ sale: s, type: 'DELETE' })}>Delete</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal show={editing !== null} title={editing?.id ? `Edit sale ${editing.invoiceNumber}` : 'New sale'} onClose={close} size="lg">
        <form onSubmit={onSubmit}>
          {formError && <div className="alert alert-danger py-2">{formError}</div>}
          <div className="row g-3">
            <FormField label="Vehicle" name="vehicleId" as="select" value={form.vehicleId} onChange={onChange}
              required disabled={Boolean(editing?.id)} col="col-12">
              <option value="">Select an available vehicle…</option>
              {vehicles.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.label || `${v.year} ${v.make} ${v.model} ${v.variant || ''} — ${v.vin} — ${formatMoney(v.price)}`}
                </option>
              ))}
            </FormField>
            <FormField label="Customer" name="customerId" as="select" value={form.customerId} onChange={onChange} required>
              <option value="">Select…</option>
              {customers.map((c) => <option key={c.id} value={c.id}>{c.fullName} ({c.phone})</option>)}
            </FormField>
            {isManager ? (
              <FormField label="Salesperson" name="salespersonId" as="select" value={form.salespersonId} onChange={onChange}>
                <option value="">Me</option>
                {staff.map((u) => <option key={u.id} value={u.id}>{u.fullName} ({titleCase(u.role)})</option>)}
              </FormField>
            ) : (
              <div className="col-md-6 small text-muted d-flex align-items-end">Salesperson: you</div>
            )}
            <FormField label="Sale price (₹)" name="salePrice" type="number" min={0} step="0.01" value={form.salePrice}
              onChange={onChange} required col="col-md-4" />
            <FormField label="Discount (₹)" name="discount" type="number" min={0} step="0.01" value={form.discount}
              onChange={onChange} col="col-md-4" />
            <div className="col-md-4">
              <label className="form-label">Final amount</label>
              <div className={`form-control-plaintext fw-semibold ${finalAmount < 0 ? 'text-danger' : ''}`}>
                {formatMoney(finalAmount)}
              </div>
            </div>
            <FormField label="Payment method" name="paymentMethod" as="select" value={form.paymentMethod} onChange={onChange} required>
              {PAYMENT_METHODS.map((p) => <option key={p} value={p}>{titleCase(p)}</option>)}
            </FormField>
            <FormField label="Sale date" name="saleDate" type="date" value={form.saleDate} onChange={onChange} />
            <FormField label="Notes" name="notes" as="textarea" value={form.notes} onChange={onChange} col="col-12" maxLength={1000} />
          </div>
          <div className="d-flex justify-content-end gap-2 mt-4">
            <button type="button" className="btn btn-light" onClick={close}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving || finalAmount < 0}>
              {saving && <span className="spinner-border spinner-border-sm me-1" />}Save
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        show={pendingAction !== null}
        title={actionText.title}
        message={pendingAction && `${pendingAction.sale.invoiceNumber}: ${actionText.msg}`}
        confirmLabel={actionText.label}
        variant={actionText.variant}
        busy={saving}
        onConfirm={runAction}
        onCancel={() => setPendingAction(null)}
      />
    </>
  );
}
