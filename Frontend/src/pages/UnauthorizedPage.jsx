import { Link } from 'react-router-dom';

const UnauthorizedPage = () => {
  return (
    <div style={{ textAlign: 'center', marginTop: '100px', fontFamily: 'sans-serif' }}>
      <h1>403 — You do not have permission to view this page</h1>
      <Link to="/dashboard" style={{ color: 'blue', textDecoration: 'underline' }}>
        Go to Dashboard
      </Link>
    </div>
  );
};

export default UnauthorizedPage;
