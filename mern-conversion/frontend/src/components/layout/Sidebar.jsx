import React from 'react';
import { Link, useLocation } from 'react-router-dom';

const Sidebar = ({ isOpen }) => {
  const location = useLocation();
  const isActive = (path) => {
    if (path === '/') return location.pathname === '/';
    return location.pathname.startsWith(path);
  };

  const storedUser = localStorage.getItem('user');
  const user = storedUser ? JSON.parse(storedUser) : { role: 'INTERN' };
  const userRole = user.role || 'INTERN';

  const allNavItems = [
    {
      to: '/',
      label: 'Dashboard',
      roles: ['INTERN', 'LEAD', 'CEO', 'ADMIN'],
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
        </svg>
      )
    },
    {
      to: '/tasks',
      label: 'My Tasks',
      roles: ['INTERN', 'LEAD', 'CEO', 'ADMIN'],
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
        </svg>
      )
    },
    {
      to: '/team',
      label: 'Team Workspace',
      roles: ['LEAD', 'CEO', 'ADMIN'], // Restricted from INTERN
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      )
    },
    {
      to: '/analytics',
      label: 'Analytics',
      roles: ['LEAD', 'CEO', 'ADMIN'], // Restricted from INTERN
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      )
    },
    {
      to: '/chat',
      label: 'Team Chat',
      roles: ['INTERN', 'LEAD', 'CEO', 'ADMIN'],
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
        </svg>
      )
    },
    {
      to: '/notifications',
      label: 'Notifications',
      roles: ['INTERN', 'LEAD', 'CEO', 'ADMIN'],
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
      )
    }
  ];

  const visibleNavItems = allNavItems.filter(item => item.roles.includes(userRole));

  return (
    <aside className={`fixed top-16 left-0 h-[calc(100vh-4rem)] w-64 bg-white dark:bg-slate-900 border-r border-gray-200 dark:border-slate-700 transition-transform duration-300 z-40 overflow-y-auto ${isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}>
      <div className="p-4">
        <div className="flex items-center justify-between pl-2 pb-2 mb-2 border-b border-gray-100 dark:border-slate-800">
          <span className="text-xs font-bold text-gray-400 dark:text-gray-500 uppercase tracking-wider">
            Workspace Hub
          </span>
          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300 uppercase">
            {userRole}
          </span>
        </div>
        <ul className="mt-2 space-y-1">
          {visibleNavItems.map((item) => {
            const active = isActive(item.to);
            return (
              <li key={item.to}>
                <Link
                  to={item.to}
                  className={`flex items-center px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                    active
                      ? 'bg-blue-50 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400 font-semibold'
                      : 'text-gray-700 hover:bg-gray-100 dark:text-slate-300 dark:hover:bg-slate-800'
                  }`}
                >
                  {item.icon}
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </div>
    </aside>
  );
};

export default Sidebar;
