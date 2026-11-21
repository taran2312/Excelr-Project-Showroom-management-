import { NavLink, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { titleCase } from '../services/format.js';
import './Navbar.css';

const links = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/vehicles', label: 'Vehicles' },
  { to: '/customers', label: 'Customers' },
  { to: '/test-drives', label: 'Test Drives' },
  { to: '/sales', label: 'Sales' },
  { to: '/showrooms', label: 'Showrooms' },
  { to: '/users', label: 'Staff', roles: ['ADMIN', 'MANAGER'] },
];

export default function Navbar() {
  const { user, logout, hasRole } = useAuth();

  return (
    <nav className="navbar navbar-expand-lg navbar-dark app-navbar">
      <div className="container-xl">
        <Link className="navbar-brand d-flex align-items-center gap-2" to="/dashboard">
          <img src="/favicon.svg" alt="" width="28" height="28" />
          <span>Showroom Manager</span>
        </Link>
        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#mainNav"
          aria-controls="mainNav"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon" />
        </button>

        <div className="collapse navbar-collapse" id="mainNav">
          <ul className="navbar-nav me-auto mb-2 mb-lg-0">
            {links
              .filter((l) => !l.roles || hasRole(...l.roles))
              .map((l) => (
                <li className="nav-item" key={l.to}>
                  <NavLink className="nav-link" to={l.to}>
                    {l.label}
                  </NavLink>
                </li>
              ))}
          </ul>

          <div className="dropdown">
            <button
              className="btn btn-outline-light btn-sm dropdown-toggle"
              type="button"
              data-bs-toggle="dropdown"
              aria-expanded="false"
            >
              {user?.fullName}
              <span className="badge text-bg-light ms-2">{titleCase(user?.role)}</span>
            </button>
            <ul className="dropdown-menu dropdown-menu-end">
              <li>
                <Link className="dropdown-item" to="/profile">
                  My profile
                </Link>
              </li>
              <li>
                <hr className="dropdown-divider" />
              </li>
              <li>
                <button className="dropdown-item text-danger" type="button" onClick={logout}>
                  Sign out
                </button>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </nav>
  );
}
