import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import PublicRoute from './components/PublicRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import UnauthorizedPage from './pages/UnauthorizedPage';
import DashboardPage from './pages/DashboardPage';

const AdminPage = () => <h1 style={{ textAlign: 'center', marginTop: '50px', fontFamily: 'sans-serif' }}>Admin Panel</h1>;
const NotFoundPage = () => <h1 style={{ textAlign: 'center', marginTop: '50px', fontFamily: 'sans-serif' }}>404 Not Found</h1>;

const App = () => {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          
          <Route path="/login" element={
            <PublicRoute>
              <LoginPage />
            </PublicRoute>
          } />
          
          <Route path="/register" element={
            <PublicRoute>
              <RegisterPage />
            </PublicRoute>
          } />
          
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          } />
          
          <Route path="/unauthorized" element={<UnauthorizedPage />} />
          
          <Route path="/admin/*" element={
            <ProtectedRoute requiredRole="ADMIN">
              <AdminPage />
            </ProtectedRoute>
          } />
          
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
};

export default App;
