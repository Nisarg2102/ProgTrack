import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { io } from 'socket.io-client';

const Header = ({ toggleSidebar, toggleTheme, theme }) => {
  const [unreadCount, setUnreadCount] = useState(0);
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);
  const [toastNotif, setToastNotif] = useState(null);
  const dropdownRef = useRef(null);
  const navigate = useNavigate();

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5001';

  // Read logged in user from localStorage
  const storedUser = localStorage.getItem('user');
  const user = storedUser ? JSON.parse(storedUser) : { fullName: 'Guest User', role: 'INTERN', email: 'guest@progtrack.com' };

  const fetchUnreadCount = async () => {
    const token = localStorage.getItem('token');
    if (!token) return;
    try {
      const res = await fetch(`${API_URL}/api/notifications/count`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        setUnreadCount(data.count || 0);
      }
    } catch (err) {
      // Ignore network errors in polling
    }
  };

  // Poll count and listen to real-time notification events
  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 15000);

    // Socket.io for immediate notification arrival
    if (user && user.email) {
      const socket = io(API_URL, {
        transports: ['websocket', 'polling']
      });

      socket.on('connect', () => {
        socket.emit('addUser', { userEmail: user.email });
      });

      socket.on('newNotification', (data) => {
        // Immediately increment/refresh the unread count
        fetchUnreadCount();

        // Show banner notification toast
        setToastNotif({
          senderName: data.senderName || data.senderEmail,
          message: data.message || 'You received a new message',
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        });

        // Auto dismiss toast after 6 seconds
        setTimeout(() => {
          setToastNotif(null);
        }, 6000);
      });

      return () => {
        clearInterval(interval);
        socket.close();
      };
    }

    return () => clearInterval(interval);
  }, [user.email, API_URL]);

  // Close dropdown on click outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setUserDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('progtrack_session_active');
    localStorage.removeItem('progtrack_session_start');
    navigate('/login');
  };

  const getInitials = (name) => {
    if (!name) return 'U';
    return name
      .split(' ')
      .map(part => part[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
  };

  return (
    <>
      <header className="fixed top-0 left-0 right-0 h-16 bg-white dark:bg-slate-900 border-b border-gray-200 dark:border-slate-700 flex items-center justify-between px-4 z-50 transition-colors duration-300">
        <div className="flex items-center gap-4">
          <Link to="/" className="text-2xl font-bold text-blue-600 dark:text-blue-500 no-underline">
            Prog<span className="text-slate-800 dark:text-slate-200">Track</span>
          </Link>
          <button onClick={toggleSidebar} className="text-gray-500 hover:text-blue-600 focus:outline-none hidden lg:block" title="Toggle Sidebar">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <button onClick={toggleSidebar} className="text-gray-500 hover:text-blue-600 focus:outline-none lg:hidden" title="Toggle Sidebar">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        </div>

        <div className="flex items-center gap-4">
          {/* Theme Toggle */}
          <button onClick={toggleTheme} className="p-2 text-gray-500 hover:text-amber-500 dark:hover:text-amber-400 rounded-lg transition-colors" title="Toggle Theme">
            {theme === 'dark' ? (
               <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                 <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
               </svg>
            ) : (
               <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                 <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
               </svg>
            )}
          </button>

          {/* Notifications Bell */}
          <Link 
            to="/notifications" 
            className="relative p-2 text-gray-500 hover:text-blue-600 dark:text-gray-400 dark:hover:text-blue-400 rounded-lg transition-colors"
            title="Notifications"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            {unreadCount > 0 && (
              <span className="absolute top-1 right-1 inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 text-[10px] font-bold text-white bg-red-600 rounded-full animate-pulse shadow-sm">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </Link>

          {/* User Profile & Dropdown */}
          <div className="relative" ref={dropdownRef}>
            <button 
              onClick={() => setUserDropdownOpen(!userDropdownOpen)}
              className="flex items-center gap-2 p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-slate-800 transition-colors focus:outline-none"
            >
              <div className="w-8 h-8 rounded-full bg-blue-600 text-white flex items-center justify-center font-bold text-xs shadow-sm">
                {getInitials(user.fullName)}
              </div>
              <div className="hidden md:flex flex-col text-left">
                <span className="text-xs font-semibold text-gray-900 dark:text-white leading-tight">
                  {user.fullName || 'User'}
                </span>
                <span className="text-[10px] text-gray-500 dark:text-gray-400 uppercase font-bold tracking-wider">
                  {user.role || 'Member'}
                </span>
              </div>
              <svg xmlns="http://www.w3.org/2000/svg" className={`h-4 w-4 text-gray-400 transition-transform ${userDropdownOpen ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            {/* Dropdown Menu */}
            {userDropdownOpen && (
              <div className="absolute right-0 mt-2 w-56 bg-white dark:bg-slate-800 rounded-xl shadow-xl border border-gray-100 dark:border-slate-700 py-2 z-50 animate-in fade-in slide-in-from-top-2">
                <div className="px-4 py-2 border-b border-gray-100 dark:border-slate-700">
                  <p className="text-sm font-semibold text-gray-900 dark:text-white">{user.fullName}</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{user.email}</p>
                  <span className="inline-block mt-1 text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300">
                    {user.role}
                  </span>
                </div>

                <div className="py-1">
                  <Link
                    to="/tasks"
                    onClick={() => setUserDropdownOpen(false)}
                    className="flex items-center px-4 py-2 text-sm text-gray-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-700/50"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2.5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                    </svg>
                    My Tasks
                  </Link>
                  <Link
                    to="/notifications"
                    onClick={() => setUserDropdownOpen(false)}
                    className="flex items-center px-4 py-2 text-sm text-gray-700 dark:text-slate-200 hover:bg-gray-50 dark:hover:bg-slate-700/50"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2.5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                    </svg>
                    Notifications ({unreadCount})
                  </Link>
                </div>

                <div className="border-t border-gray-100 dark:border-slate-700 pt-1">
                  <button
                    onClick={handleLogout}
                    className="w-full flex items-center px-4 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                    Sign Out
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Floating Notification Toast */}
      {toastNotif && (
        <div className="fixed top-20 right-4 z-50 max-w-sm w-full bg-white dark:bg-slate-800 rounded-xl shadow-2xl border border-blue-200 dark:border-blue-900/60 p-4 animate-in slide-in-from-top-4 transition-all">
          <div className="flex items-start gap-3">
            <div className="w-9 h-9 rounded-full bg-blue-100 dark:bg-blue-900/50 text-blue-600 flex items-center justify-center flex-shrink-0">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between">
                <h4 className="text-xs font-bold text-blue-600 dark:text-blue-400 uppercase tracking-wide">
                  New Private Message
                </h4>
                <button
                  onClick={() => setToastNotif(null)}
                  className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 text-sm font-bold ml-1"
                >
                  &times;
                </button>
              </div>
              <p className="text-xs font-semibold text-gray-900 dark:text-white mt-0.5 truncate">
                {toastNotif.senderName}
              </p>
              <p className="text-xs text-gray-600 dark:text-gray-300 mt-1 line-clamp-2">
                {toastNotif.message}
              </p>
              <div className="mt-2 flex items-center gap-2">
                <button
                  onClick={() => {
                    setToastNotif(null);
                    navigate('/chat');
                  }}
                  className="text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 px-3 py-1 rounded-md transition-colors"
                >
                  Open Chat &rarr;
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Header;
