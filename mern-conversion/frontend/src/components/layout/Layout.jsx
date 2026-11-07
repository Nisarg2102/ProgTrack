import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Header from './Header';
import Sidebar from './Sidebar';
import Footer from './Footer';

const Layout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [theme, setTheme] = useState(localStorage.getItem('theme') || 'light');

  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleSidebar = () => setSidebarOpen(!sidebarOpen);
  const toggleTheme = () => setTheme(theme === 'dark' ? 'light' : 'dark');

  return (
    <div className="min-h-screen flex flex-col bg-gray-50 dark:bg-slate-900 transition-colors duration-300 text-slate-900 dark:text-slate-100">
      <Header toggleSidebar={toggleSidebar} toggleTheme={toggleTheme} theme={theme} />
      <Sidebar isOpen={sidebarOpen} />
      
      <main className={`flex-1 flex flex-col pt-16 transition-all duration-300 ${sidebarOpen ? 'lg:ml-64' : 'lg:ml-64'}`}>
        <div className="flex-1 p-6 overflow-x-hidden">
          <Outlet />
        </div>
        <Footer />
      </main>
    </div>
  );
};
export default Layout;
