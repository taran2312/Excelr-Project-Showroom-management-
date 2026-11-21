import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import Loader from './Loader.jsx';

/** Guards nested routes. Without `roles` it only requires login; with `roles` it also checks the role. */
export default function ProtectedRoute({ roles }) {
  const { isAuthenticated, initializing, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  if (initializing) {
    return <Loader full />;
  }
  if (roles && (!user || !roles.includes(user.role))) {
    return (
      <div className="container py-5 text-center">
        <h4>Access denied</h4>
        <p className="text-muted">Your role does not have access to this page.</p>
      </div>
    );
  }
  return <Outlet />;
}
