import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import Layout from './components/layout/Layout';
import Dashboard from './pages/Dashboard';
import Tasks from './pages/Tasks';
import Team from './pages/Team';
import Analytics from './pages/Analytics';
import Chat from './pages/Chat';
import Notifications from './pages/Notifications';

// Authenticated Route wrapper
const ProtectedRoute = ({ children }) => {
  const token = localStorage.getItem('token');
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

// Role-based Access Route wrapper
const RoleRoute = ({ children, allowedRoles }) => {
  const storedUser = localStorage.getItem('user');
  const user = storedUser ? JSON.parse(storedUser) : null;
  const userRole = user?.role || 'INTERN';

  if (!allowedRoles.includes(userRole)) {
    // If not authorized, redirect to home dashboard
    return <Navigate to="/" replace />;
  }
  return children;
};

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Dashboard />} />
          <Route path="tasks" element={<Tasks />} />
          
          {/* Team Workspace - Only Lead, CEO, and Admin */}
          <Route 
            path="team" 
            element={
              <RoleRoute allowedRoles={['LEAD', 'CEO', 'ADMIN']}>
                <Team />
              </RoleRoute>
            } 
          />

          {/* Analytics - Only Lead, CEO, and Admin */}
          <Route 
            path="analytics" 
            element={
              <RoleRoute allowedRoles={['LEAD', 'CEO', 'ADMIN']}>
                <Analytics />
              </RoleRoute>
            } 
          />

          <Route path="chat" element={<Chat />} />
          <Route path="notifications" element={<Notifications />} />
        </Route>
        
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}

export default App;
