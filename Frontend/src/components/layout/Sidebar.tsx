import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  FolderOpen,
  Upload,
  Activity,
  Settings,
  Shield,
} from 'lucide-react';
import { cn } from '../../utils/helpers';

interface NavItem {
  name: string;
  path: string;
  icon: React.ReactNode;
}

const navItems: NavItem[] = [
  {
    name: 'Dashboard',
    path: '/dashboard',
    icon: <LayoutDashboard className="w-5 h-5" />,
  },
  {
    name: 'My Files',
    path: '/files',
    icon: <FolderOpen className="w-5 h-5" />,
  },
  {
    name: 'Upload',
    path: '/upload',
    icon: <Upload className="w-5 h-5" />,
  },
  {
    name: 'Activity Logs',
    path: '/activity',
    icon: <Activity className="w-5 h-5" />,
  },
  {
    name: 'Access Control',
    path: '/access-control',
    icon: <Shield className="w-5 h-5" />,
  },
  {
    name: 'Settings',
    path: '/settings',
    icon: <Settings className="w-5 h-5" />,
  },
];

export const Sidebar: React.FC = () => {
  return (
    <aside className="w-72 bg-surface border-r-2 border-border-strong h-[calc(100vh-4.5625rem)] sticky top-[4.5625rem] shadow-2xl">
      <nav className="p-6 space-y-2">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-4 px-5 py-3.5 rounded-xl transition-all duration-300 font-medium',
                'text-text-secondary hover:text-text-primary hover:bg-surface-elevated hover:translate-x-1',
                isActive &&
                  'bg-gradient-to-r from-primary to-primary-hover text-white shadow-xl shadow-primary/30 translate-x-1'
              )
            }
          >
            {item.icon}
            <span>{item.name}</span>
          </NavLink>
        ))}
      </nav>

      {/* Storage Info */}
      <div className="absolute bottom-6 left-6 right-6">
        <div className="bg-surface-elevated rounded-xl p-5 border-2 border-border shadow-lg">
          <div className="flex items-center justify-between mb-3">
            <span className="text-sm font-semibold text-text-secondary">Storage Used</span>
            <span className="text-sm font-bold text-text-primary">
              2.4 GB / 10 GB
            </span>
          </div>
          <div className="w-full h-2.5 bg-background-secondary rounded-full overflow-hidden shadow-inner">
            <div
              className="h-full bg-gradient-to-r from-primary to-secondary rounded-full transition-all duration-500 shadow-lg shadow-primary/50"
              style={{ width: '24%' }}
            />
          </div>
          <button className="w-full mt-4 py-2 text-sm font-semibold text-primary hover:text-primary-hover transition-all duration-200 hover:scale-105">
            Upgrade Storage →
          </button>
        </div>
      </div>
    </aside>
  );
};
