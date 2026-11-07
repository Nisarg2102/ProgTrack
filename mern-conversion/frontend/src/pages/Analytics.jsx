import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
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
  Legend
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

const Analytics = () => {
  const [activeTab, setActiveTab] = useState('PRODUCTIVITY'); // PRODUCTIVITY or ATTENDANCE
  const [productivity, setProductivity] = useState(null);
  const [attendance, setAttendance] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5001';

  const storedUser = localStorage.getItem('user');
  const user = storedUser ? JSON.parse(storedUser) : { role: 'LEAD' };
  const canViewAttendance = user.role === 'CEO' || user.role === 'ADMIN';

  const fetchData = async () => {
    setLoading(true);
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    try {
      const prodRes = await fetch(`${API_URL}/api/analytics/productivity`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (prodRes.ok) {
        const prodData = await prodRes.json();
        setProductivity(prodData);
      }

      if (canViewAttendance) {
        const attRes = await fetch(`${API_URL}/api/analytics/attendance`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (attRes.ok) {
          const attData = await attRes.json();
          setAttendance(attData);
        }
      }
    } catch (err) {
      console.error('Failed to fetch analytics:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Prepare Chart Data
  const summary = productivity?.summary || {
    totalTasks: 0,
    completedTasks: 0,
    inProgressTasks: 0,
    pendingTasks: 0,
    blockedTasks: 0,
    completionRate: 0
  };

  const statusChartData = {
    labels: ['Completed', 'In Progress', 'Pending', 'Blocked'],
    datasets: [
      {
        data: [
          summary.completedTasks,
          summary.inProgressTasks,
          summary.pendingTasks,
          summary.blockedTasks
        ],
        backgroundColor: ['#22c55e', '#3b82f6', '#f59e0b', '#ef4444'],
        borderWidth: 0
      }
    ]
  };

  // 7-day trend data
  const trendLabels = productivity?.weeklyTrend?.map(t => t._id) || ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const trendCompleted = productivity?.weeklyTrend?.map(t => t.completed) || [0, 0, 0, 0, 0, 0, 0];
  const trendTotal = productivity?.weeklyTrend?.map(t => t.total) || [0, 0, 0, 0, 0, 0, 0];

  const trendChartData = {
    labels: trendLabels.length > 0 ? trendLabels : ['Today'],
    datasets: [
      {
        label: 'Total Tasks',
        data: trendTotal.length > 0 ? trendTotal : [summary.totalTasks],
        borderColor: '#3b82f6',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        tension: 0.3,
        fill: true
      },
      {
        label: 'Completed Tasks',
        data: trendCompleted.length > 0 ? trendCompleted : [summary.completedTasks],
        borderColor: '#22c55e',
        backgroundColor: 'rgba(34, 197, 94, 0.1)',
        tension: 0.3,
        fill: true
      }
    ]
  };

  // Attendance hours per member chart
  const members = attendance?.attendanceSummary || [];
  const hoursChartData = {
    labels: members.map(m => m.name || m.email),
    datasets: [
      {
        label: 'Total Logged Hours',
        data: members.map(m => m.totalHours),
        backgroundColor: '#3b82f6',
        borderRadius: 6
      }
    ]
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      
      {/* Title Header */}
      <div className="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 p-6 flex flex-wrap gap-4 items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-7 w-7 text-blue-600 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
            Analytics & Executive Intelligence
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Data-driven insights into team velocity, task throughput, attendance, and work hours.
          </p>
        </div>

        {/* Tab switch */}
        {canViewAttendance && (
          <div className="inline-flex rounded-lg border border-gray-200 dark:border-slate-700 bg-gray-50 dark:bg-slate-900 p-1 text-xs font-semibold">
            <button
              onClick={() => setActiveTab('PRODUCTIVITY')}
              className={`px-4 py-2 rounded-md transition-colors ${activeTab === 'PRODUCTIVITY' ? 'bg-blue-600 text-white shadow-sm' : 'text-gray-600 dark:text-gray-400 hover:text-gray-900'}`}
            >
              Productivity & Tasks
            </button>
            <button
              onClick={() => setActiveTab('ATTENDANCE')}
              className={`px-4 py-2 rounded-md transition-colors ${activeTab === 'ATTENDANCE' ? 'bg-blue-600 text-white shadow-sm' : 'text-gray-600 dark:text-gray-400 hover:text-gray-900'}`}
            >
              Attendance & Work Hours
            </button>
          </div>
        )}
      </div>

      {loading ? (
        <div className="bg-white dark:bg-slate-800 rounded-xl p-12 text-center text-gray-500">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-600 border-t-transparent mb-2"></div>
          <p className="text-sm">Aggregating analytics metrics...</p>
        </div>
      ) : activeTab === 'PRODUCTIVITY' ? (
        <>
          {/* KPI Metrics */}
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
            <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-gray-100 dark:border-slate-700 shadow-sm">
              <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400">Total Tasks</span>
              <p className="text-2xl font-bold text-gray-900 dark:text-white mt-1">{summary.totalTasks}</p>
            </div>
            <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-gray-100 dark:border-slate-700 shadow-sm">
              <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400">Completion Rate</span>
              <p className="text-2xl font-bold text-green-600 dark:text-green-400 mt-1">{summary.completionRate}%</p>
            </div>
            <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-gray-100 dark:border-slate-700 shadow-sm">
              <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400">In Progress</span>
              <p className="text-2xl font-bold text-blue-600 dark:text-blue-400 mt-1">{summary.inProgressTasks}</p>
            </div>
            <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-gray-100 dark:border-slate-700 shadow-sm">
              <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400">Pending</span>
              <p className="text-2xl font-bold text-amber-500 mt-1">{summary.pendingTasks}</p>
            </div>
            <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-gray-100 dark:border-slate-700 shadow-sm col-span-2 md:col-span-1">
              <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400">Blocked</span>
              <p className="text-2xl font-bold text-red-600 dark:text-red-400 mt-1">{summary.blockedTasks}</p>
            </div>
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="bg-white dark:bg-slate-800 p-6 rounded-xl border border-gray-100 dark:border-slate-700 shadow-sm lg:col-span-2">
              <h3 className="text-base font-bold text-gray-900 dark:text-white mb-4">Task Activity Velocity</h3>
              <div className="h-64">
                <Line
                  data={trendChartData}
                  options={{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { position: 'bottom' } },
                    scales: { y: { beginAtZero: true } }
                  }}
                />
              </div>
            </div>

            <div className="bg-white dark:bg-slate-800 p-6 rounded-xl border border-gray-100 dark:border-slate-700 shadow-sm">
              <h3 className="text-base font-bold text-gray-900 dark:text-white mb-4">Status Distribution</h3>
              <div className="h-64 flex items-center justify-center">
                {summary.totalTasks === 0 ? (
                  <p className="text-sm text-gray-400">No tasks logged yet</p>
                ) : (
                  <Doughnut
                    data={statusChartData}
                    options={{
                      responsive: true,
                      maintainAspectRatio: false,
                      plugins: { legend: { position: 'bottom' } }
                    }}
                  />
                )}
              </div>
            </div>
          </div>
        </>
      ) : (
        <>
          {/* ATTENDANCE TAB */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="bg-white dark:bg-slate-800 p-6 rounded-xl border border-gray-100 dark:border-slate-700 shadow-sm lg:col-span-3">
              <h3 className="text-base font-bold text-gray-900 dark:text-white mb-4">Logged Work Hours per Member</h3>
              <div className="h-64">
                <Bar
                  data={hoursChartData}
                  options={{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: { y: { beginAtZero: true, title: { display: true, text: 'Hours' } } }
                  }}
                />
              </div>
            </div>
          </div>

          <div className="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-gray-100 dark:border-slate-700 p-6">
            <h3 className="text-base font-bold text-gray-900 dark:text-white mb-4">Member Attendance Ledger</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-gray-200 dark:border-slate-700 text-gray-400 text-xs uppercase tracking-wider">
                    <th className="py-3 px-3">Team Member</th>
                    <th className="py-3 px-3">Department</th>
                    <th className="py-3 px-3">Role</th>
                    <th className="py-3 px-3 text-right">Total Hours</th>
                    <th className="py-3 px-3 text-right">Work Sessions</th>
                    <th className="py-3 px-3 text-right">Avg Session Hours</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 dark:divide-slate-800">
                  {members.map((m, idx) => (
                    <tr key={idx} className="hover:bg-gray-50/50 dark:hover:bg-slate-750">
                      <td className="py-3 px-3">
                        <span className="font-semibold text-gray-900 dark:text-white">{m.name}</span>
                        <span className="block text-xs text-gray-400">{m.email}</span>
                      </td>
                      <td className="py-3 px-3 text-gray-600 dark:text-gray-300">{m.department}</td>
                      <td className="py-3 px-3">
                        <span className="bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300 px-2 py-0.5 rounded text-[10px] font-bold uppercase">
                          {m.role}
                        </span>
                      </td>
                      <td className="py-3 px-3 text-right font-mono font-bold text-blue-600 dark:text-blue-400">
                        {m.totalHours} hrs
                      </td>
                      <td className="py-3 px-3 text-right text-gray-600 dark:text-gray-300 font-mono">
                        {m.sessionsCount}
                      </td>
                      <td className="py-3 px-3 text-right text-gray-600 dark:text-gray-300 font-mono">
                        {m.avgHoursSession} hrs
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

    </div>
  );
};

export default Analytics;
