import { useEffect } from 'react';
import './Modal.css';

/** React-controlled modal using Bootstrap's markup (no Bootstrap JS needed). */
export default function Modal({ show, title, onClose, children, footer, size = '' }) {
  useEffect(() => {
    if (!show) return undefined;
    const onKey = (e) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    document.body.classList.add('modal-open');
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.classList.remove('modal-open');
    };
  }, [show, onClose]);

  if (!show) return null;

  return (
    <>
      <div className="modal d-block app-modal" tabIndex="-1" role="dialog" aria-modal="true" onMouseDown={onClose}>
        <div
          className={`modal-dialog modal-dialog-scrollable modal-dialog-centered ${size ? `modal-${size}` : ''}`}
          onMouseDown={(e) => e.stopPropagation()}
        >
          <div className="modal-content">
            <div className="modal-header">
              <h5 className="modal-title">{title}</h5>
              <button type="button" className="btn-close" aria-label="Close" onClick={onClose} />
            </div>
            <div className="modal-body">{children}</div>
            {footer && <div className="modal-footer">{footer}</div>}
          </div>
        </div>
      </div>
      <div className="modal-backdrop fade show" />
    </>
  );
}
