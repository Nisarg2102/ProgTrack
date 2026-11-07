import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const defaultTaskRow = () => ({
  taskDescription: '',
  taskCategory: 'Development',
  priority: 'MEDIUM',
  status: 'IN_PROGRESS',
  timeSpent: 60,
  googleDocLink: ''
});

const Tasks = () => {
  const [activeTab, setActiveTab] = useState('TODAY'); // TODAY or HISTORY
  const [todayTasks, setTodayTasks] = useState([defaultTaskRow()]);
  const [historyTasks, setHistoryTasks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [sessionLoading, setSessionLoading] = useState(false);
  
  // Persistent timer state initialized from localStorage
  const [sessionActive, setSessionActive] = useState(() => {
    return localStorage.getItem('progtrack_session_active') === 'true';
  });
  const [sessionStartTime, setSessionStartTime] = useState(() => {
    return localStorage.getItem('progtrack_session_start') || null;
  });
  const [sessionSeconds, setSessionSeconds] = useState(() => {
    const start = localStorage.getItem('progtrack_session_start');
    if (localStorage.getItem('progtrack_session_active') === 'true' && start) {
      return Math.max(0, Math.floor((Date.now() - new Date(start).getTime()) / 1000));
    }
    return 0;
  });

  const navigate = useNavigate();
  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5001';

  // Sync active session from backend on mount and window focus
  const fetchActiveSession = async () => {
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
      const res = await fetch(`${API_URL}/api/worklogs/active`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        if (data.active && data.loginDateTime) {
          setSessionActive(true);
          setSessionStartTime(data.loginDateTime);
          localStorage.setItem('progtrack_session_active', 'true');
          localStorage.setItem('progtrack_session_start', data.loginDateTime);
          setSessionSeconds(data.elapsedSeconds || Math.max(0, Math.floor((Date.now() - new Date(data.loginDateTime).getTime()) / 1000)));
        } else {
          setSessionActive(false);
          setSessionStartTime(null);
          setSessionSeconds(0);
          localStorage.removeItem('progtrack_session_active');
          localStorage.removeItem('progtrack_session_start');
        }
      }
    } catch (err) {
      console.error('Failed to sync active session:', err);
    }
  };

  useEffect(() => {
    fetchActiveSession();
    // Also re-sync when switching back to tab / app window
    const handleFocus = () => {
      fetchActiveSession();
    };
    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, []);

  // Load today's tasks
  const fetchTodayTasks = async () => {
    setLoading(true);
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    try {
      const res = await fetch(`${API_URL}/api/tasks/today`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        if (data && data.length > 0) {
          setTodayTasks(data);
        } else {
          // Initialize with exactly 1 empty draft row
          setTodayTasks([defaultTaskRow()]);
        }
      }
    } catch (err) {
      console.error('Failed to fetch today tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  // Load task history
  const fetchTaskHistory = async () => {
    setLoading(true);
    const token = localStorage.getItem('token');
    try {
      const res = await fetch(`${API_URL}/api/tasks/history`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        setHistoryTasks(data);
      }
    } catch (err) {
      console.error('Failed to fetch task history:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'TODAY') {
      fetchTodayTasks();
    } else {
      fetchTaskHistory();
    }
  }, [activeTab]);

  // Session timer ticker (wall-clock based so it never drifts in background tabs or apps)
  useEffect(() => {
    if (!sessionActive || !sessionStartTime) return;

    const updateTimer = () => {
      const startMs = new Date(sessionStartTime).getTime();
      const nowMs = Date.now();
      const elapsed = Math.max(0, Math.floor((nowMs - startMs) / 1000));
      setSessionSeconds(elapsed);
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);
    return () => clearInterval(interval);
  }, [sessionActive, sessionStartTime]);

  const toggleSession = async () => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    setSessionLoading(true);
    setErrorMessage('');

    if (!sessionActive) {
      try {
        const res = await fetch(`${API_URL}/api/worklogs/start`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          }
        });
        if (res.ok) {
          const data = await res.json();
          const startTime = data.loginDateTime || new Date().toISOString();
          setSessionActive(true);
          setSessionStartTime(startTime);
          localStorage.setItem('progtrack_session_active', 'true');
          localStorage.setItem('progtrack_session_start', startTime);
          setSessionSeconds(data.elapsedSeconds || 0);
        } else {
          const errData = await res.json().catch(() => ({}));
          setErrorMessage(errData.message || 'Failed to start session. Please try again.');
        }
      } catch (e) {
        console.error('Failed to start session:', e);
        setErrorMessage('Network error while starting session.');
      } finally {
        setSessionLoading(false);
      }
    } else {
      try {
        const res = await fetch(`${API_URL}/api/worklogs/end`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          }
        });
        if (res.ok) {
          setSessionActive(false);
          setSessionStartTime(null);
          setSessionSeconds(0);
          localStorage.removeItem('progtrack_session_active');
          localStorage.removeItem('progtrack_session_start');
        } else {
          const errData = await res.json().catch(() => ({}));
          setErrorMessage(errData.message || 'Failed to end session. Please try again.');
        }
      } catch (e) {
        console.error('Failed to end session:', e);
        setErrorMessage('Network error while ending session.');
      } finally {
        setSessionLoading(false);
      }
    }
  };

  const handleTaskChange = (index, field, value) => {
    setTodayTasks(prev => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  };

  const addRow = () => {
    setTodayTasks(prev => [
      ...prev,
      { taskDescription: '', taskCategory: 'Development', priority: 'MEDIUM', status: 'IN_PROGRESS', timeSpent: 30, googleDocLink: '' }
    ]);
  };

  const removeRow = (index) => {
    if (todayTasks.length <= 1) {
      setTodayTasks([defaultTaskRow()]);
      return;
    }
    setTodayTasks(prev => prev.filter((_, i) => i !== index));
  };

  const submitGrid = async (e) => {
    e.preventDefault();
    setSubmitSuccess('');
    setErrorMessage('');

    // Filter valid tasks
    const validTasks = todayTasks.filter(t => t.taskDescription && t.taskDescription.trim().length > 0);
    if (validTasks.length === 0) {
      setErrorMessage('Please enter at least one task description.');
      return;
    }

    const token = localStorage.getItem('token');
    try {
      const res = await fetch(`${API_URL}/api/tasks/submit-daily`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          taskDate: new Date(),
          tasks: validTasks
        })
      });

      if (res.ok) {
        setSubmitSuccess('Daily task report successfully submitted!');
        fetchTodayTasks();
      } else {
        const data = await res.json();
        setErrorMessage(data.message || 'Failed to submit tasks');
      }
    } catch (err) {
      setErrorMessage('Network error while submitting report.');
    }
  };

  const formatTimer = (totalSeconds) => {
    const hrs = Math.floor(totalSeconds / 3600);
    const mins = Math.floor((totalSeconds % 3600) / 60);
    const secs = totalSeconds % 60;
    return `${hrs.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      
      {/* Top Banner & Session Tracker */}
      <div className="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 p-6 flex flex-wrap gap-4 items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-7 w-7 text-blue-600 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            My Workspace & Daily Tasks
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Log your daily activities, track working sessions, and submit your progress reports.
          </p>
        </div>

        {/* Work session tracker card */}
        <div className="flex items-center gap-4 bg-gray-50 dark:bg-slate-900 border border-gray-200 dark:border-slate-700 px-4 py-2.5 rounded-xl">
          <div>
            <span className="block text-[10px] uppercase font-bold text-gray-400 tracking-wider">Session Time</span>
            <span className="text-xl font-mono font-bold text-gray-800 dark:text-gray-100">{formatTimer(sessionSeconds)}</span>
          </div>
          <button
            type="button"
            onClick={toggleSession}
            disabled={sessionLoading}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors disabled:opacity-50 flex items-center gap-1 ${
              sessionActive 
                ? 'bg-red-600 hover:bg-red-700 text-white' 
                : 'bg-green-600 hover:bg-green-700 text-white'
            }`}
          >
            {sessionLoading ? (
              <span>Processing...</span>
            ) : sessionActive ? (
              <span>End Session</span>
            ) : (
              <span>Clock In</span>
            )}
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200 dark:border-slate-700">
        <button
          onClick={() => setActiveTab('TODAY')}
          className={`py-3 px-6 text-sm font-semibold border-b-2 transition-colors ${
            activeTab === 'TODAY'
              ? 'border-blue-600 text-blue-600 dark:text-blue-400'
              : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'
          }`}
        >
          Today's Task Grid
        </button>
        <button
          onClick={() => setActiveTab('HISTORY')}
          className={`py-3 px-6 text-sm font-semibold border-b-2 transition-colors ${
            activeTab === 'HISTORY'
              ? 'border-blue-600 text-blue-600 dark:text-blue-400'
              : 'border-transparent text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'
          }`}
        >
          Submission History
        </button>
      </div>

      {/* Alerts */}
      {submitSuccess && (
        <div className="bg-green-50 border border-green-300 text-green-800 px-4 py-3 rounded-lg flex items-center justify-between">
          <span>{submitSuccess}</span>
          <button onClick={() => setSubmitSuccess('')} className="font-bold">&times;</button>
        </div>
      )}
      {errorMessage && (
        <div className="bg-red-50 border border-red-300 text-red-800 px-4 py-3 rounded-lg flex items-center justify-between">
          <span>{errorMessage}</span>
          <button onClick={() => setErrorMessage('')} className="font-bold">&times;</button>
        </div>
      )}

      {/* TAB 1: TODAY'S GRID */}
      {activeTab === 'TODAY' && (
        <div className="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 p-6">
          <form onSubmit={submitGrid} className="space-y-4">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="border-b border-gray-200 dark:border-slate-700 text-gray-500 dark:text-gray-400 text-xs uppercase tracking-wider">
                    <th className="py-3 px-2 w-10">#</th>
                    <th className="py-3 px-2 min-w-[280px]">Task Description</th>
                    <th className="py-3 px-2 w-36">Category</th>
                    <th className="py-3 px-2 w-32">Priority</th>
                    <th className="py-3 px-2 w-36">Status</th>
                    <th className="py-3 px-2 w-28">Minutes</th>
                    <th className="py-3 px-2 min-w-[180px]">Artifact / Doc Link</th>
                    <th className="py-3 px-2 w-12 text-center">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 dark:divide-slate-800">
                  {todayTasks.map((task, idx) => (
                    <tr key={idx} className="hover:bg-gray-50/50 dark:hover:bg-slate-750">
                      <td className="py-3 px-2 font-mono text-xs text-gray-400">{idx + 1}</td>
                      <td className="py-3 px-2">
                        <input
                          type="text"
                          required
                          placeholder="e.g. Implemented JWT auth endpoint..."
                          value={task.taskDescription || ''}
                          onChange={(e) => handleTaskChange(idx, 'taskDescription', e.target.value)}
                          className="w-full text-sm rounded-md border border-gray-300 dark:border-slate-600 dark:bg-slate-900 dark:text-white px-3 py-1.5 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        />
                      </td>
                      <td className="py-3 px-2">
                        <select
                          value={task.taskCategory || 'Development'}
                          onChange={(e) => handleTaskChange(idx, 'taskCategory', e.target.value)}
                          className="w-full text-xs rounded-md border border-gray-300 dark:border-slate-600 dark:bg-slate-900 dark:text-white px-2 py-1.5 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        >
                          <option value="Development">Development</option>
                          <option value="Bugfix">Bugfix</option>
                          <option value="Testing">Testing</option>
                          <option value="Documentation">Documentation</option>
                          <option value="Research">Research</option>
                          <option value="Meeting">Meeting</option>
                        </select>
                      </td>
                      <td className="py-3 px-2">
                        <select
                          value={task.priority || 'MEDIUM'}
                          onChange={(e) => handleTaskChange(idx, 'priority', e.target.value)}
                          className="w-full text-xs rounded-md border border-gray-300 dark:border-slate-600 dark:bg-slate-900 dark:text-white px-2 py-1.5 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        >
                          <option value="LOW">Low</option>
                          <option value="MEDIUM">Medium</option>
                          <option value="HIGH">High</option>
                          <option value="URGENT">Urgent</option>
                        </select>
                      </td>
                      <td className="py-3 px-2">
                        <select
                          value={task.status || 'IN_PROGRESS'}
                          onChange={(e) => handleTaskChange(idx, 'status', e.target.value)}
                          className="w-full text-xs rounded-md border border-gray-300 dark:border-slate-600 dark:bg-slate-900 dark:text-white px-2 py-1.5 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        >
                          <option value="IN_PROGRESS">In Progress</option>
                          <option value="COMPLETED">Completed</option>
                          <option value="PENDING">Pending</option>
                          <option value="BLOCKED">Blocked</option>
                        </select>
                      </td>
                      <td className="py-3 px-2">
                        <input
                          type="number"
                          min="0"
                          step="15"
                          value={task.timeSpent || 0}
                          onChange={(e) => handleTaskChange(idx, 'timeSpent', Number(e.target.value))}
                          className="w-full text-xs rounded-md border border-gray-300 dark:border-slate-600 dark:bg-slate-900 dark:text-white px-2 py-1.5 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        />
                      </td>
                      <td className="py-3 px-2">
                        <input
                          type="text"
                          placeholder="https://docs.google.com/..."
                          value={task.googleDocLink || ''}
                          onChange={(e) => handleTaskChange(idx, 'googleDocLink', e.target.value)}
                          className="w-full text-xs rounded-md border border-gray-300 dark:border-slate-600 dark:bg-slate-900 dark:text-white px-2 py-1.5 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        />
                      </td>
                      <td className="py-3 px-2 text-center">
                        <button
                          type="button"
                          onClick={() => removeRow(idx)}
                          className="text-gray-400 hover:text-red-500 p-1"
                          title="Remove row"
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between pt-4 border-t border-gray-100 dark:border-slate-700">
              <button
                type="button"
                onClick={addRow}
                className="inline-flex items-center text-sm font-semibold text-blue-600 dark:text-blue-400 hover:underline"
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z" clipRule="evenodd" />
                </svg>
                Add Another Task
              </button>

              <button
                type="submit"
                className="bg-blue-600 hover:bg-blue-700 text-white font-semibold text-sm px-6 py-2.5 rounded-lg shadow-sm transition-colors flex items-center"
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-2" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
                Submit Daily Grid
              </button>
            </div>
          </form>
        </div>
      )}

      {/* TAB 2: SUBMISSION HISTORY */}
      {activeTab === 'HISTORY' && (
        <div className="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 p-6">
          <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Past 30 Days Activity</h3>

          {loading ? (
            <div className="text-center py-8 text-gray-500">Loading history...</div>
          ) : historyTasks.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              <p>No submitted tasks in history yet. Submit your daily grid to populate history!</p>
            </div>
          ) : (
            <div className="space-y-4">
              {historyTasks.map((t) => (
                <div
                  key={t._id}
                  className="p-4 border border-gray-100 dark:border-slate-700 rounded-lg flex flex-wrap gap-4 items-center justify-between bg-gray-50/50 dark:bg-slate-900/50"
                >
                  <div className="flex-1 min-w-[240px]">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs font-mono text-gray-400">
                        {new Date(t.taskDate).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })}
                      </span>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                        t.status === 'COMPLETED' ? 'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-300' :
                        t.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300' :
                        t.status === 'BLOCKED' ? 'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-300' :
                        'bg-amber-100 text-amber-700 dark:bg-amber-900/50 dark:text-amber-300'
                      }`}>
                        {t.status}
                      </span>
                      <span className="text-[10px] bg-gray-200 dark:bg-slate-700 text-gray-700 dark:text-gray-300 px-2 py-0.5 rounded">
                        {t.taskCategory || 'General'}
                      </span>
                    </div>
                    <p className="text-sm font-medium text-gray-900 dark:text-white">{t.taskDescription}</p>
                    {t.googleDocLink && (
                      <a href={t.googleDocLink} target="_blank" rel="noreferrer" className="text-xs text-blue-500 hover:underline inline-block mt-1">
                        View Attached Document &rarr;
                      </a>
                    )}
                  </div>

                  <div className="text-right">
                    <span className="text-sm font-bold text-gray-700 dark:text-gray-300">{t.timeSpent || 0} mins</span>
                    <span className="block text-[10px] uppercase font-semibold text-gray-400">Time Spent</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

    </div>
  );
};

export default Tasks;
