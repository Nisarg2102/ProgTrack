const WorkLog = require('../models/WorkLog');
const User = require('../models/User');

const getMyWorkLog = async (req, res) => {
  try {
    const { from, to } = req.query;
    const endDate = to ? new Date(to) : new Date();
    endDate.setHours(23, 59, 59, 999);
    
    const startDate = from ? new Date(from) : new Date(endDate);
    if (!from) startDate.setDate(startDate.getDate() - 29);
    startDate.setHours(0, 0, 0, 0);

    const workLogs = await WorkLog.find({
      user: req.user._id,
      loginDateTime: { $gte: startDate, $lte: endDate }
    }).sort({ loginDateTime: -1 });

    res.json(workLogs);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const getActiveSession = async (req, res) => {
  try {
    const activeLog = await WorkLog.findOne({ user: req.user._id, logoutDateTime: null }).sort({ loginDateTime: -1 });
    if (!activeLog) {
      return res.json({ active: false, elapsedSeconds: 0, loginDateTime: null });
    }
    const elapsedSeconds = Math.max(0, Math.floor((Date.now() - new Date(activeLog.loginDateTime).getTime()) / 1000));
    res.json({
      active: true,
      workLogId: activeLog._id,
      loginDateTime: activeLog.loginDateTime,
      elapsedSeconds
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const startSession = async (req, res) => {
  try {
    let activeLog = await WorkLog.findOne({ user: req.user._id, logoutDateTime: null }).sort({ loginDateTime: -1 });
    if (activeLog) {
      const elapsedSeconds = Math.max(0, Math.floor((Date.now() - new Date(activeLog.loginDateTime).getTime()) / 1000));
      return res.status(200).json({
        active: true,
        workLogId: activeLog._id,
        loginDateTime: activeLog.loginDateTime,
        elapsedSeconds
      });
    }

    const workLog = await WorkLog.create({
      user: req.user._id,
      loginDateTime: new Date(),
      ipAddress: req.ip
    });

    res.status(201).json({
      active: true,
      workLogId: workLog._id,
      loginDateTime: workLog.loginDateTime,
      elapsedSeconds: 0
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const endSession = async (req, res) => {
  try {
    const now = new Date();
    const openLogs = await WorkLog.find({ user: req.user._id, logoutDateTime: null });
    if (!openLogs || openLogs.length === 0) {
      return res.json({ message: 'No active session found', active: false });
    }

    let lastTotalSeconds = 0;
    for (const log of openLogs) {
      log.logoutDateTime = now;
      log.calculateTotalSeconds();
      await log.save();
      lastTotalSeconds = log.totalSeconds;
    }

    res.json({
      message: 'Session ended successfully',
      active: false,
      totalSeconds: lastTotalSeconds,
      logoutDateTime: now
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

module.exports = {
  getMyWorkLog,
  getActiveSession,
  startSession,
  endSession
};
