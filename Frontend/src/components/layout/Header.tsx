import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { User, LogOut, Bell, Search } from 'lucide-react';
import { Button } from '../ui';
import { getUser, removeAuthToken, removeUser } from '../../utils/helpers';

export const Header: React.FC = () => {
  const navigate = useNavigate();
  const user = getUser();

  const handleLogout = () => {
    removeAuthToken();
    removeUser();
    navigate('/login');
  };

  return (
    <header className="sticky top-0 z-40 bg-surface/95 border-b border-border-strong backdrop-blur-xl shadow-lg">
      <div className="px-8 py-4">
        <div className="flex items-center justify-between">
          {/* Logo and Brand */}
          <Link to="/dashboard" className="flex items-center gap-3 group">
            <div className="w-11 h-11 bg-gradient-to-br from-primary to-secondary rounded-xl flex items-center justify-center group-hover:scale-110 group-hover:shadow-xl group-hover:shadow-primary/50 transition-all duration-300">
              <svg
                className="w-6 h-6 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"
                />
              </svg>
            </div>
            <div>
              <h1 className="text-xl font-bold text-text-primary tracking-tight">CloudVault</h1>
              <p className="text-xs text-text-muted">Secure Document Sharing</p>
            </div>
          </Link>

          {/* Search Bar */}
          <div className="flex-1 max-w-2xl mx-12">
            <div className="relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-text-muted" />
              <input
                type="text"
                placeholder="Search files, documents, folders..."
                className="w-full pl-11 pr-4 py-3 bg-surface border-2 border-border rounded-xl text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-all duration-200"
              />
            </div>
          </div>

          {/* Right Section */}
          <div className="flex items-center gap-4">
            {/* Notifications */}
            <button className="relative p-2.5 text-text-muted hover:text-text-primary hover:bg-surface-elevated rounded-xl transition-all duration-200 hover:scale-105">
              <Bell className="w-5 h-5" />
              <span className="absolute top-2 right-2 w-2 h-2 bg-primary rounded-full ring-2 ring-surface"></span>
            </button>

            {/* User Profile */}
            <div className="flex items-center gap-3 pl-4 border-l-2 border-border">
              <div className="text-right">
                <p className="text-sm font-medium text-text-primary">
                  {user?.name || 'User'}
                </p>
                <p className="text-xs text-text-muted">{user?.role || 'USER'}</p>
              </div>
              <div className="w-10 h-10 bg-accent rounded-full flex items-center justify-center">
                <User className="w-5 h-5 text-primary" />
              </div>
            </div>

            {/* Logout Button */}
            <Button
              variant="secondary"
              size="sm"
              icon={<LogOut className="w-4 h-4" />}
              onClick={handleLogout}
            >
              Logout
            </Button>
          </div>
        </div>
      </div>
    </header>
  );
};
