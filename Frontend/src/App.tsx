import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/layout';
import {
  LoginPage,
  RegisterPage,
  DashboardPage,
  FilesPage,
  UploadPage,
  ActivityPage,
  AccessControlPage,
  SettingsPage,
} from './pages';

function App() {
  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Protected Routes */}
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="files" element={<FilesPage />} />
          <Route path="upload" element={<UploadPage />} />
          <Route path="activity" element={<ActivityPage />} />
          <Route path="access-control" element={<AccessControlPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
