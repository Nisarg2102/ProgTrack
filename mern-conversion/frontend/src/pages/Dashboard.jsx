import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { Line, Bar, Doughnut } from 'react-chartjs-2';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend
);

const Dashboard = () => {
  const [metrics, setMetrics] = useState(null);
  const [recentTasks, setRecentTasks] = useState([]);
  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5001';

  const storedUser = localStorage.getItem('user');
  const user = storedUser ? JSON.parse(storedUser) : { fullName: 'Member', role: 'INTERN' };

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return;

    // Fetch productivity metrics
    fetch(`${API_URL}/api/analytics/productivity`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data) setMetrics(data);
      })
      .catch(() => {});

    // Fetch recent tasks
    fetch(`${API_URL}/api/tasks/history`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data && data.length > 0) setRecentTasks(data.slice(0, 5));
      })
      .catch(() => {});
  }, []);

  const summary = metrics?.summary || {
    totalTasks: 124,
    completedTasks: 85,
    completionRate: 68,
    inProgressTasks: 25,
    pendingTasks: 14
  };

  const trendData = {
    labels: metrics?.weeklyTrend?.length > 0
      ? metrics.weeklyTrend.map(t => t._id)
      : ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
    datasets: [
      {
        label: 'Tasks Completed',
        data: metrics?.weeklyTrend?.length > 0
          ? metrics.weeklyTrend.map(t => t.completed)
          : [12, 19, 15, 25, 22, 10, 5],
        borderColor: '#4154f1',
        backgroundColor: 'rgba(65,84,241,0.08)',
        fill: true,
        tension: 0.4
      }
    ]
  };

  const statusData = {
    labels: ['Completed', 'In Progress', 'Pending', 'Blocked'],
    datasets: [
      {
        data: [
          summary.completedTasks || 45,
          summary.inProgressTasks || 25,
          summary.pendingTasks || 20,
          summary.blockedTasks || 10
        ],
        backgroundColor: ['#22c55e', '#4154f1', '#f59e0b', '#ef4444'],
        borderWidth: 0
      }
    ]
  };

  const deptData = {
    labels: ['Development', 'QA', 'Design', 'Marketing', 'Sales'],
    datasets: [
      {
        label: 'Tasks',
        data: [65, 40, 25, 15, 10],
        backgroundColor: ['#4154f1', '#22c55e', '#f59e0b', '#ef4444', '#06b6d4'],
        borderRadius: 4
      }
    ]
  };

  return (
    <div className="space-y-6">
      
      {/* Header Banner */}
      <div className="flex flex-wrap justify-between items-center gap-4 bg-white dark:bg-slate-800 p-6 rounded-xl border border-gray-100 dark:border-slate-700 shadow-sm">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
            Workspace Dashboard
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Welcome back, <strong className="text-blue-600 dark:text-blue-400">{user.fullName}</strong> ({user.role})
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link
            to="/tasks"
            className="bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold px-4 py-2.5 rounded-lg shadow-sm transition-colors flex items-center"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 mr-1.5" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z" clipRule="evenodd" />
            </svg>
            Add Daily Task
          </Link>
          <Link
            to="/team"
            className="bg-gray-100 hover:bg-gray-200 dark:bg-slate-700 dark:hover:bg-slate-600 text-gray-700 dark:text-slate-200 text-xs font-semibold px-4 py-2.5 rounded-lg transition-colors"
          >
            Team Status
          </Link>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-slate-800 rounded-xl p-6 shadow-sm border-t-4 border-blue-600 border border-gray-100 dark:border-slate-700">
          <div className="flex justify-between items-start">
            <div>
              <div className="text-3xl font-bold text-gray-800 dark:text-white">{summary.totalTasks}</div>
              <div className="text-xs font-bold text-gray-500 dark:text-gray-400 mt-1 uppercase tracking-wider">Total Tasks</div>
            </div>
            <div className="w-12 h-12 rounded-full bg-blue-50 dark:bg-blue-900/30 flex items-center justify-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-blue-600 dark:text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
            </div>
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-xl p-6 shadow-sm border-t-4 border-green-500 border border-gray-100 dark:border-slate-700">
          <div className="flex justify-between items-start">
            <div>
              <div className="text-3xl font-bold text-gray-800 dark:text-white">{summary.completedTasks}</div>
              <div className="text-xs font-bold text-gray-500 dark:text-gray-400 mt-1 uppercase tracking-wider">Completed</div>
            </div>
            <div className="w-12 h-12 rounded-full bg-green-50 dark:bg-green-900/30 flex items-center justify-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-green-500 dark:text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
            </div>
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-xl p-6 shadow-sm border-t-4 border-amber-500 border border-gray-100 dark:border-slate-700">
          <div className="flex justify-between items-start">
            <div>
              <div className="text-3xl font-bold text-gray-800 dark:text-white">{summary.completionRate}%</div>
              <div className="text-xs font-bold text-gray-500 dark:text-gray-400 mt-1 uppercase tracking-wider">Completion Rate</div>
            </div>
            <div className="w-12 h-12 rounded-full bg-amber-50 dark:bg-amber-900/30 flex items-center justify-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-amber-500 dark:text-amber-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
            </div>
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-xl p-6 shadow-sm border-t-4 border-cyan-500 border border-gray-100 dark:border-slate-700">
          <div className="flex justify-between items-start">
            <div>
              <div className="text-3xl font-bold text-gray-800 dark:text-white">34.5h</div>
              <div className="text-xs font-bold text-gray-500 dark:text-gray-400 mt-1 uppercase tracking-wider">Hours Logged</div>
            </div>
            <div className="w-12 h-12 rounded-full bg-cyan-50 dark:bg-cyan-900/30 flex items-center justify-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-cyan-500 dark:text-cyan-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white dark:bg-slate-800 rounded-xl p-6 shadow-sm border border-gray-100 dark:border-slate-700">
          <h2 className="text-lg font-bold mb-4 text-gray-800 dark:text-white">Company Task Trend</h2>
          <div className="h-64">
            <Line data={trendData} options={{ maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { grid: { color: 'rgba(150,150,150,0.1)' } }, x: { grid: { display: false } } } }} />
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-xl p-6 shadow-sm border border-gray-100 dark:border-slate-700">
          <h2 className="text-lg font-bold mb-4 text-gray-800 dark:text-white">Department Task Distribution</h2>
          <div className="h-64">
            <Bar data={deptData} options={{ indexAxis: 'y', maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { x: { grid: { color: 'rgba(150,150,150,0.1)' } }, y: { grid: { display: false } } } }} />
          </div>
        </div>
      </div>

      {/* Status & Recent Submissions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="bg-white dark:bg-slate-800 rounded-xl p-6 shadow-sm border border-gray-100 dark:border-slate-700">
          <h2 className="text-lg font-bold mb-4 text-gray-800 dark:text-white">Status Distribution</h2>
          <div className="h-64 flex justify-center">
            <Doughnut data={statusData} options={{ maintainAspectRatio: false, cutout: '65%', plugins: { legend: { position: 'bottom', labels: { color: '#64748b' } } } }} />
          </div>
        </div>

        <div className="col-span-1 lg:col-span-2 bg-white dark:bg-slate-800 rounded-xl p-6 shadow-sm border border-gray-100 dark:border-slate-700">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-bold text-gray-800 dark:text-white">Recent Task Submissions</h2>
            <Link to="/tasks" className="text-xs font-semibold text-blue-600 hover:underline">
              View All &rarr;
            </Link>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="text-xs text-gray-500 dark:text-gray-400 uppercase bg-gray-50 dark:bg-slate-700/50">
                <tr>
                  <th className="px-4 py-3 rounded-tl-lg">Task</th>
                  <th className="px-4 py-3">Category</th>
                  <th className="px-4 py-3">Time</th>
                  <th className="px-4 py-3 rounded-tr-lg">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-slate-700/50">
                {recentTasks.length > 0 ? (
                  recentTasks.map((t, idx) => (
                    <tr key={idx} className="hover:bg-gray-50 dark:hover:bg-slate-700/25 transition-colors">
                      <td className="px-4 py-3 font-medium text-gray-900 dark:text-white">{t.taskDescription}</td>
                      <td className="px-4 py-3 text-gray-500 dark:text-gray-400">{t.taskCategory || 'General'}</td>
                      <td className="px-4 py-3 text-gray-500 dark:text-gray-400">{t.timeSpent || 0}m</td>
                      <td className="px-4 py-3">
                        <span className={`text-xs font-medium px-2.5 py-0.5 rounded ${
                          t.status === 'COMPLETED' ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400' :
                          t.status === 'IN_PROGRESS' ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400' :
                          'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400'
                        }`}>
                          {t.status}
                        </span>
                      </td>
                    </tr>
                  ))
                ) : (
                  <>
                    <tr className="hover:bg-gray-50 dark:hover:bg-slate-700/25 transition-colors">
                      <td className="px-4 py-3 font-medium text-gray-900 dark:text-white">Implemented JWT auth endpoint</td>
                      <td className="px-4 py-3 text-gray-500 dark:text-gray-400">Development</td>
                      <td className="px-4 py-3 text-gray-500 dark:text-gray-400">120m</td>
                      <td className="px-4 py-3"><span className="bg-green-100 text-green-800 text-xs font-medium px-2.5 py-0.5 rounded dark:bg-green-900/30 dark:text-green-400 border border-green-200 dark:border-green-800">Completed</span></td>
                    </tr>
                    <tr className="hover:bg-gray-50 dark:hover:bg-slate-700/25 transition-colors">
                      <td className="px-4 py-3 font-medium text-gray-900 dark:text-white">Built Analytics & Attendance Ledger</td>
                      <td className="px-4 py-3 text-gray-500 dark:text-gray-400">Analytics</td>
                      <td className="px-4 py-3 text-gray-500 dark:text-gray-400">90m</td>
                      <td className="px-4 py-3"><span className="bg-blue-100 text-blue-800 text-xs font-medium px-2.5 py-0.5 rounded dark:bg-blue-900/30 dark:text-blue-400 border border-blue-200 dark:border-blue-800">In Progress</span></td>
                    </tr>
                    <tr className="hover:bg-gray-50 dark:hover:bg-slate-700/25 transition-colors">
                      <td className="px-4 py-3 font-medium text-gray-900 dark:text-white">Notification Realtime Hub</td>
                      <td className="px-4 py-3 text-gray-500 dark:text-gray-400">QA</td>
                      <td className="px-4 py-3 text-gray-500 dark:text-gray-400">60m</td>
                      <td className="px-4 py-3"><span className="bg-amber-100 text-amber-800 text-xs font-medium px-2.5 py-0.5 rounded dark:bg-amber-900/30 dark:text-amber-400 border border-amber-200 dark:border-amber-800">Pending</span></td>
                    </tr>
                  </>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

    </div>
  );
};

export default Dashboard;
