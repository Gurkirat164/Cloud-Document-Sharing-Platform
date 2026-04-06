import { Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/login/LoginPage'
import RegisterPage from './pages/register/RegisterPage'
import DashboardPage from './pages/dashboard/DashboardPage'
import FilesPage from './pages/files/FilesPage'
import SharedPage from './pages/shared/SharedPage'
import ActivityPage from './pages/activity/ActivityPage'
import ProfilePage from './pages/profile/ProfilePage'
import { useAuth } from './context/AuthContext'

function PrivateRoute({ children }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/" element={<PrivateRoute><DashboardPage /></PrivateRoute>} />
      <Route path="/files" element={<PrivateRoute><FilesPage /></PrivateRoute>} />
      <Route path="/shared" element={<PrivateRoute><SharedPage /></PrivateRoute>} />
      <Route path="/activity" element={<PrivateRoute><ActivityPage /></PrivateRoute>} />
      <Route path="/profile" element={<PrivateRoute><ProfilePage /></PrivateRoute>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
