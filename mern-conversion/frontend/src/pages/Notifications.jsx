import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const Notifications = () => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL'); // ALL or UNREAD
  const navigate = useNavigate();

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5001';

  const fetchNotifications = async () => {
    setLoading(true);
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    try {
      const res = await fetch(`${API_URL}/api/notifications`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        setNotifications(data);
      }
    } catch (err) {
      console.error('Failed to fetch notifications:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const markAsRead = async (id, e) => {
    if (e) e.stopPropagation();
    const token = localStorage.getItem('token');
    try {
      const res = await fetch(`${API_URL}/api/notifications/mark-read/${id}`, {
        method: 'PUT',
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        setNotifications(prev =>
          prev.map(n => n._id === id ? { ...n, isSeen: true } : n)
        );
      }
    } catch (err) {
      console.error('Error marking notification read:', err);
    }
  };

  const markAllRead = async () => {
    const token = localStorage.getItem('token');
    try {
      const res = await fetch(`${API_URL}/api/notifications/mark-all-read`, {
        method: 'PUT',
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        setNotifications(prev => prev.map(n => ({ ...n, isSeen: true })));
      }
    } catch (err) {
      console.error('Error marking all notifications read:', err);
    }
  };

  const handleClick = (notif) => {
    if (!notif.isSeen) {
      markAsRead(notif._id);
    }
    if (notif.actionUrl && notif.actionUrl !== '#' && notif.actionUrl !== 'null') {
      navigate(notif.actionUrl);
    }
  };

  const unseenCount = notifications.filter(n => !n.isSeen).length;
  const displayedNotifications = filter === 'UNREAD' 
    ? notifications.filter(n => !n.isSeen)
    : notifications;

  const formatTime = (dateStr) => {
    if (!dateStr) return 'Just now';
    const d = new Date(dateStr);
    return d.toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 overflow-hidden">
        
        {/* Header */}
        <div className="p-6 border-b border-gray-100 dark:border-slate-700 flex flex-wrap gap-4 justify-between items-center bg-gray-50/50 dark:bg-slate-800/50">
          <div>
            <h2 className="text-xl font-bold flex items-center text-gray-900 dark:text-white">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-blue-600 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
              Notifications Hub
              {unseenCount > 0 && (
                <span className="ml-3 bg-red-500 text-white text-xs font-bold px-2 py-0.5 rounded-full shadow-sm">
                  {unseenCount} Unread
                </span>
              )}
            </h2>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Stay updated with system broadcasts, report updates, and announcements.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <div className="inline-flex rounded-lg border border-gray-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-0.5 text-xs font-medium">
              <button
                onClick={() => setFilter('ALL')}
                className={`px-3 py-1.5 rounded-md transition-colors ${filter === 'ALL' ? 'bg-blue-600 text-white' : 'text-gray-600 dark:text-gray-400 hover:text-gray-900'}`}
              >
                All ({notifications.length})
              </button>
              <button
                onClick={() => setFilter('UNREAD')}
                className={`px-3 py-1.5 rounded-md transition-colors ${filter === 'UNREAD' ? 'bg-blue-600 text-white' : 'text-gray-600 dark:text-gray-400 hover:text-gray-900'}`}
              >
                Unread ({unseenCount})
              </button>
            </div>

            <button 
              onClick={markAllRead}
              disabled={unseenCount === 0}
              className="text-xs bg-blue-600 text-white px-3 py-2 rounded-md font-semibold shadow-sm hover:bg-blue-700 disabled:opacity-50 transition-colors flex items-center"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
              </svg>
              Mark All Read
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="p-6">
          {loading ? (
            <div className="text-center py-12 text-gray-500">
              <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-600 border-t-transparent mb-2"></div>
              <p className="text-sm">Loading notifications...</p>
            </div>
          ) : displayedNotifications.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              <div className="bg-gray-100 dark:bg-slate-700 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-3">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
              </div>
              <h3 className="text-base font-bold text-gray-900 dark:text-white">
                {filter === 'UNREAD' ? 'No unread notifications' : 'No notifications yet'}
              </h3>
              <p className="text-xs text-gray-500 mt-1">You're all caught up!</p>
            </div>
          ) : (
            <div className="space-y-3">
              {displayedNotifications.map(notif => (
                <div 
                  key={notif._id}
                  onClick={() => handleClick(notif)}
                  className={`flex items-start gap-4 p-4 rounded-xl border cursor-pointer transition-all ${
                    !notif.isSeen 
                      ? 'bg-blue-50/50 dark:bg-blue-950/20 border-blue-200 dark:border-blue-900/60 shadow-sm relative overflow-hidden' 
                      : 'bg-white dark:bg-slate-900 border-gray-100 dark:border-slate-800 hover:border-gray-200'
                  }`}
                >
                  {!notif.isSeen && <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-blue-600"></div>}
                  
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${
                    !notif.isSeen ? 'bg-blue-100 dark:bg-blue-900/50 text-blue-600 dark:text-blue-400' : 'bg-gray-100 dark:bg-slate-800 text-gray-500'
                  }`}>
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                    </svg>
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <p className={`text-sm text-gray-900 dark:text-white ${!notif.isSeen ? 'font-semibold' : ''}`}>
                      {notif.message}
                    </p>
                    <div className="flex items-center gap-3 text-xs text-gray-500 dark:text-gray-400 mt-2">
                      <span>{formatTime(notif.createdAt)}</span>
                      <span className="bg-gray-100 dark:bg-slate-800 text-gray-600 dark:text-gray-300 px-2 py-0.5 rounded text-[10px] font-semibold">
                        {notif.type}
                      </span>
                      {!notif.isSeen && (
                        <span className="bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300 px-2 py-0.5 rounded-full text-[10px] font-bold">
                          Unread
                        </span>
                      )}
                    </div>
                  </div>
                  
                  {!notif.isSeen && (
                    <button 
                      onClick={(e) => markAsRead(notif._id, e)}
                      className="flex-shrink-0 text-xs font-semibold text-blue-600 hover:text-blue-700 bg-white dark:bg-slate-800 px-2.5 py-1 rounded border border-blue-200 dark:border-slate-700 hover:bg-blue-50 transition-colors"
                      title="Mark as Read"
                    >
                      Read
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Notifications;
