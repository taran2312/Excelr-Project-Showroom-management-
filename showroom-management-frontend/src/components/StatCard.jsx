import './StatCard.css';

export default function StatCard({ label, value, hint, accent = 'primary' }) {
  return (
    <div className={`card stat-card stat-${accent} h-100`}>
      <div className="card-body">
        <div className="stat-label">{label}</div>
        <div className="stat-value">{value}</div>
        {hint && <div className="stat-hint">{hint}</div>}
      </div>
    </div>
  );
}
