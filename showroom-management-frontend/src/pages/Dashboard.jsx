import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import StatCard from '../components/StatCard.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import Loader from '../components/Loader.jsx';
import PageHeader from '../components/PageHeader.jsx';
import EmptyState from '../components/EmptyState.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import dashboardService from '../services/dashboardService.js';
import { getErrorMessage } from '../services/api.js';
import { formatDate, formatMoney } from '../services/format.js';

export default function Dashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    dashboardService
      .getStats()
      .then(setStats)
      .catch((err) => setError(getErrorMessage(err)));
  }, []);

  if (error) return <div className="alert alert-danger">{error}</div>;
  if (!stats) return <Loader />;

  return (
    <>
      <PageHeader title={`Welcome, ${user?.fullName?.split(' ')[0] || ''}`} subtitle="Showroom overview" />

      <div className="row g-3 mb-4">
        <div className="col-6 col-lg-3">
          <StatCard label="Vehicles in stock" value={stats.availableVehicles}
            hint={`${stats.totalVehicles} total across ${stats.totalShowrooms} showroom(s)`} accent="success" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Reserved" value={stats.reservedVehicles} hint={`${stats.pendingSales} pending sale(s)`}
            accent="warning" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Sold" value={stats.soldVehicles} hint={`${stats.completedSales} completed sale(s)`}
            accent="secondary" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Revenue" value={formatMoney(stats.totalRevenue)} hint="Completed sales, after discount" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Customers" value={stats.totalCustomers} accent="info" />
        </div>
        <div className="col-6 col-lg-3">
          <StatCard label="Upcoming test drives" value={stats.upcomingTestDrives} accent="info" />
        </div>
      </div>

      <div className="card content-card">
        <div className="card-header d-flex justify-content-between align-items-center">
          <span className="fw-semibold">Recent sales</span>
          <Link to="/sales" className="small">View all</Link>
        </div>
        {stats.recentSales.length === 0 ? (
          <EmptyState message="No sales recorded yet." />
        ) : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>Invoice</th>
                  <th>Vehicle</th>
                  <th>Customer</th>
                  <th className="text-end">Amount</th>
                  <th>Date</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {stats.recentSales.map((s) => (
                  <tr key={s.id}>
                    <td className="font-monospace small">{s.invoiceNumber}</td>
                    <td>{s.vehicleLabel}</td>
                    <td>{s.customerName}</td>
                    <td className="text-end">{formatMoney(s.finalAmount)}</td>
                    <td>{formatDate(s.saleDate)}</td>
                    <td><StatusBadge status={s.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
