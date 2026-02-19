import React from 'react';
import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import { Sidebar } from './Sidebar';

export const Layout: React.FC = () => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background-secondary to-background">
      <Header />
      <div className="flex">
        <Sidebar />
        <main className="flex-1 p-10 max-w-[1800px] mx-auto w-full">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
