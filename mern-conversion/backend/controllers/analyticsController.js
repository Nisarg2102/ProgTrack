const DailyTask = require('../models/DailyTask');
const WorkLog = require('../models/WorkLog');
const User = require('../models/User');

const getProductivityAnalytics = async (req, res) => {
  try {
    const totalTasks = await DailyTask.countDocuments();
    const completedTasks = await DailyTask.countDocuments({ status: 'COMPLETED' });
    const inProgressTasks = await DailyTask.countDocuments({ status: 'IN_PROGRESS' });
    const pendingTasks = await DailyTask.countDocuments({ status: 'PENDING' });
    const blockedTasks = await DailyTask.countDocuments({ status: 'BLOCKED' });

    // Category breakdown
    const categories = await DailyTask.aggregate([
      { $group: { _id: '$taskCategory', count: { $sum: 1 } } }
    ]);

    // Priority breakdown
    const priorities = await DailyTask.aggregate([
      { $group: { _id: '$priority', count: { $sum: 1 } } }
    ]);

    // 7-day trend
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 6);
    sevenDaysAgo.setHours(0, 0, 0, 0);

    const weeklyTrend = await DailyTask.aggregate([
      { $match: { taskDate: { $gte: sevenDaysAgo } } },
      {
        $group: {
          _id: { $dateToString: { format: '%Y-%m-%d', date: '$taskDate' } },
          completed: {
            $sum: { $cond: [{ $eq: ['$status', 'COMPLETED'] }, 1, 0] }
          },
          total: { $sum: 1 }
        }
      },
      { $sort: { _id: 1 } }
    ]);

    res.json({
      summary: {
        totalTasks,
        completedTasks,
        inProgressTasks,
        pendingTasks,
        blockedTasks,
        completionRate: totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0
      },
      categories,
      priorities,
      weeklyTrend
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const getAttendanceAnalytics = async (req, res) => {
  try {
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    // Fetch users with their work logs
    const users = await User.find({ isActive: true }).select('fullName email department designation role');

    const logs = await WorkLog.find({ loginDateTime: { $gte: thirtyDaysAgo } })
      .populate('user', 'fullName department');

    // Aggregate hours per user
    const userHoursMap = {};
    users.forEach(u => {
      userHoursMap[u._id.toString()] = {
        user: u,
        totalSeconds: 0,
        sessionsCount: 0
      };
    });

    logs.forEach(log => {
      if (log.user && userHoursMap[log.user._id?.toString()]) {
        userHoursMap[log.user._id.toString()].totalSeconds += (log.totalSeconds || 0);
        userHoursMap[log.user._id.toString()].sessionsCount += 1;
      }
    });

    const userAttendance = Object.values(userHoursMap).map(item => ({
      userId: item.user._id,
      name: item.user.fullName,
      email: item.user.email,
      department: item.user.department || 'General',
      designation: item.user.designation || 'Staff',
      role: item.user.role,
      totalHours: Math.round((item.totalSeconds / 3600) * 10) / 10,
      sessionsCount: item.sessionsCount,
      avgHoursSession: item.sessionsCount > 0 ? Math.round((item.totalSeconds / 3600 / item.sessionsCount) * 10) / 10 : 0
    }));

    res.json({
      totalActiveUsers: users.length,
      attendanceSummary: userAttendance
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

module.exports = {
  getProductivityAnalytics,
  getAttendanceAnalytics
};
