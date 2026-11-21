export default function Loader({ full = false, text = 'Loading…' }) {
  return (
    <div className={`d-flex justify-content-center align-items-center ${full ? 'vh-100' : 'py-5'}`}>
      <div className="spinner-border text-primary me-2" role="status" aria-hidden="true" />
      <span className="text-muted">{text}</span>
    </div>
  );
}
