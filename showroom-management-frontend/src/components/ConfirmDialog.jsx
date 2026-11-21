import Modal from './Modal.jsx';

export default function ConfirmDialog({
  show,
  title = 'Are you sure?',
  message,
  confirmLabel = 'Delete',
  variant = 'danger',
  busy = false,
  onConfirm,
  onCancel,
}) {
  return (
    <Modal
      show={show}
      title={title}
      onClose={onCancel}
      footer={
        <>
          <button className="btn btn-light" onClick={onCancel} disabled={busy}>
            Cancel
          </button>
          <button className={`btn btn-${variant}`} onClick={onConfirm} disabled={busy}>
            {busy && <span className="spinner-border spinner-border-sm me-1" />}
            {confirmLabel}
          </button>
        </>
      }
    >
      <p className="mb-0">{message}</p>
    </Modal>
  );
}
