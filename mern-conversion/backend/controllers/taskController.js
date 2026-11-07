const DailyTask = require('../models/DailyTask');
const User = require('../models/User');

const getTodayTasks = async (req, res) => {
  try {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const tasks = await DailyTask.find({
      user: req.user._id,
      taskDate: { $gte: today, $lt: tomorrow }
    }).populate('user', 'fullName department');

    res.json(tasks);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const submitDailyGrid = async (req, res) => {
  try {
    const { taskDate, tasks } = req.body;
    const date = taskDate ? new Date(taskDate) : new Date();

    const startOfDay = new Date(date);
    startOfDay.setHours(0, 0, 0, 0);
    const endOfDay = new Date(date);
    endOfDay.setHours(23, 59, 59, 999);

    // Delete existing unsubmitted tasks for the date
    await DailyTask.deleteMany({
      user: req.user._id,
      taskDate: { $gte: startOfDay, $lte: endOfDay },
      submitted: false
    });

    const savedTasks = [];
    for (const dto of tasks) {
      if (!dto.taskDescription || dto.taskDescription.trim() === '') continue;

      const task = await DailyTask.create({
        user: req.user._id,
        taskDate: date,
        taskDescription: dto.taskDescription.trim(),
        timeSpent: dto.timeSpent || 0,
        status: dto.status || 'IN_PROGRESS',
        googleDocLink: dto.googleDocLink,
        taskCategory: dto.taskCategory,
        priority: dto.priority || 'MEDIUM',
        submitted: true
      });
      savedTasks.push(task);
    }

    res.status(201).json(savedTasks);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const getTaskHistory = async (req, res) => {
  try {
    const { from, to } = req.query;
    const endDate = to ? new Date(to) : new Date();
    endDate.setHours(23, 59, 59, 999);
    
    const startDate = from ? new Date(from) : new Date(endDate);
    if (!from) startDate.setDate(startDate.getDate() - 29);
    startDate.setHours(0, 0, 0, 0);

    const tasks = await DailyTask.find({
      user: req.user._id,
      taskDate: { $gte: startDate, $lte: endDate }
    }).populate('user', 'fullName department').sort({ taskDate: -1 });

    res.json(tasks);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const getTeamTasks = async (req, res) => {
  try {
    const { date } = req.query;
    const targetDate = date ? new Date(date) : new Date();
    targetDate.setHours(0, 0, 0, 0);
    const nextDate = new Date(targetDate);
    nextDate.setDate(nextDate.getDate() + 1);

    let tasks = [];
    if (req.user.role === 'CEO' || req.user.role === 'ADMIN') {
      tasks = await DailyTask.find({
        taskDate: { $gte: targetDate, $lt: nextDate }
      }).populate('user', 'fullName department designation');
    } else {
      // Find users in the same department
      const teamMembers = await User.find({ department: req.user.department });
      const memberIds = teamMembers.map(u => u._id);
      
      tasks = await DailyTask.find({
        user: { $in: memberIds },
        taskDate: { $gte: targetDate, $lt: nextDate }
      }).populate('user', 'fullName department designation');
    }

    res.json(tasks);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const deleteTask = async (req, res) => {
  try {
    const task = await DailyTask.findById(req.params.id);
    if (!task) return res.status(404).json({ message: 'Task not found' });
    
    if (task.user.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Not authorized' });
    }
    
    if (task.submitted) {
      return res.status(400).json({ message: 'Cannot delete a submitted task' });
    }

    await DailyTask.findByIdAndDelete(req.params.id);
    res.json({ message: 'Task deleted' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

module.exports = {
  getTodayTasks,
  submitDailyGrid,
  getTaskHistory,
  getTeamTasks,
  deleteTask
};
