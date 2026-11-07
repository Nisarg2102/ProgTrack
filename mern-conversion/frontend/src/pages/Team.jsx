import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const Team = () => {
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5001';

  const fetchTeamTasks = async (date) => {
    setLoading(true);
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    try {
      const res = await fetch(`${API_URL}/api/tasks/team?date=${date}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        setTasks(data);
      }
    } catch (err) {
      console.error('Failed to fetch team tasks:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTeamTasks(selectedDate);
  }, [selectedDate]);

  // Aggregate by user
  const userTaskMap = {};
  tasks.forEach(t => {
    const userKey = t.user?._id || 'unassigned';
    if (!userTaskMap[userKey]) {
      userTaskMap[userKey] = {
        user: t.user || { fullName: 'Team Member', department: 'General', designation: 'Staff' },
        tasks: [],
        totalMinutes: 0
      };
    }
    userTaskMap[userKey].tasks.push(t);
    userTaskMap[userKey].totalMinutes += (t.timeSpent || 0);
  });

  const memberList = Object.values(userTaskMap);
  const totalCompleted = tasks.filter(t => t.status === 'COMPLETED').length;
  const totalHours = Math.round((tasks.reduce((acc, t) => acc + (t.timeSpent || 0), 0) / 60) * 10) / 10;

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      
      {/* Header & Date Filter */}
      <div className="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 p-6 flex flex-wrap gap-4 items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-7 w-7 text-blue-600 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
            Team Workspace Reports
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Monitor department tasks, reviews, and daily progress submissions across the team.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <label htmlFor="teamDate" className="text-xs font-semibold text-gray-600 dark:text-gray-300">
            Select Date:
          </label>
          <input
            id="teamDate"
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="text-sm border border-gray-300 dark:border-slate-600 rounded-lg px-3 py-1.5 dark:bg-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-gray-100 dark:border-slate-700 shadow-sm">
          <span className="text-xs uppercase font-bold text-gray-400">Total Team Tasks</span>
          <p className="text-2xl font-bold text-gray-900 dark:text-white mt-1">{tasks.length}</p>
        </div>
        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-gray-100 dark:border-slate-700 shadow-sm">
          <span className="text-xs uppercase font-bold text-gray-400">Tasks Completed</span>
          <p className="text-2xl font-bold text-green-600 dark:text-green-400 mt-1">
            {totalCompleted} / {tasks.length}
          </p>
        </div>
        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-gray-100 dark:border-slate-700 shadow-sm">
          <span className="text-xs uppercase font-bold text-gray-400">Total Time Logged</span>
          <p className="text-2xl font-bold text-blue-600 dark:text-blue-400 mt-1">{totalHours} hrs</p>
        </div>
      </div>

      {/* Team Tasks Content */}
      <div className="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 p-6">
        <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">
          Member Submissions for {selectedDate}
        </h2>

        {loading ? (
          <div className="text-center py-12 text-gray-500">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-600 border-t-transparent mb-2"></div>
            <p className="text-sm">Loading team tasks...</p>
          </div>
        ) : memberList.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <div className="bg-gray-100 dark:bg-slate-700 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-3">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
            </div>
            <h3 className="text-base font-bold text-gray-900 dark:text-white">No tasks submitted for this date</h3>
            <p className="text-xs text-gray-500 mt-1">Team members haven't submitted reports for {selectedDate} yet.</p>
          </div>
        ) : (
          <div className="space-y-6">
            {memberList.map((memberGroup, idx) => (
              <div key={idx} className="border border-gray-100 dark:border-slate-700 rounded-xl overflow-hidden">
                <div className="bg-gray-50 dark:bg-slate-900/60 p-4 border-b border-gray-100 dark:border-slate-700 flex flex-wrap gap-2 justify-between items-center">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full bg-blue-600 text-white font-bold flex items-center justify-center text-sm">
                      {memberGroup.user.fullName ? memberGroup.user.fullName[0].toUpperCase() : 'M'}
                    </div>
                    <div>
                      <h3 className="font-bold text-sm text-gray-900 dark:text-white">
                        {memberGroup.user.fullName}
                      </h3>
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        {memberGroup.user.designation || 'Specialist'} &bull; {memberGroup.user.department || 'IT'}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className="text-xs font-semibold text-gray-700 dark:text-gray-300">
                      {memberGroup.tasks.length} tasks &bull; {Math.round((memberGroup.totalMinutes / 60) * 10) / 10} hrs logged
                    </span>
                  </div>
                </div>

                <div className="p-4 space-y-3">
                  {memberGroup.tasks.map((task, tIdx) => (
                    <div key={tIdx} className="flex flex-wrap items-center justify-between gap-2 p-3 bg-white dark:bg-slate-800 rounded-lg border border-gray-100 dark:border-slate-700/60">
                      <div className="flex-1 min-w-[200px]">
                        <p className="text-sm font-medium text-gray-900 dark:text-white">{task.taskDescription}</p>
                        <div className="flex items-center gap-2 mt-1">
                          <span className="text-[10px] bg-gray-100 dark:bg-slate-700 text-gray-600 dark:text-gray-300 px-2 py-0.5 rounded">
                            {task.taskCategory || 'General'}
                          </span>
                          <span className="text-[10px] text-gray-400">
                            Priority: {task.priority || 'MEDIUM'}
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-4">
                        <span className={`text-xs font-bold px-2.5 py-1 rounded-full ${
                          task.status === 'COMPLETED' ? 'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-300' :
                          task.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300' :
                          task.status === 'BLOCKED' ? 'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-300' :
                          'bg-amber-100 text-amber-700 dark:bg-amber-900/50 dark:text-amber-300'
                        }`}>
                          {task.status}
                        </span>
                        <span className="text-xs font-mono font-bold text-gray-500 dark:text-gray-400">
                          {task.timeSpent || 0}m
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

    </div>
  );
};

export default Team;
