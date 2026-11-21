import './Toasts.css';

export default function Toasts({ toasts, onDismiss }) {
  return (
    <div className="toast-stack" aria-live="polite">
      {toasts.map((t) => (
        <div key={t.id} className={`toast show align-items-center text-bg-${t.variant} border-0`} role="alert">
          <div className="d-flex">
            <div className="toast-body">{t.message}</div>
            <button
              type="button"
              className="btn-close btn-close-white me-2 m-auto"
              aria-label="Close"
              onClick={() => onDismiss(t.id)}
            />
          </div>
        </div>
      ))}
    </div>
  );
}
