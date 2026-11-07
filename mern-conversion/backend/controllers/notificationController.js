const Notification = require('../models/Notification');

const getNotifications = async (req, res) => {
  try {
    let notifications = await Notification.find({ user: req.user._id })
      .sort({ createdAt: -1 })
      .limit(50);

    // If user has 0 notifications, seed a few helpful onboarding notifications
    if (notifications.length === 0) {
      const defaultNotifs = [
        {
          user: req.user._id,
          message: `Welcome to ProgTrack, ${req.user.fullName}! Complete your daily workspace tasks on time.`,
          type: 'ANNOUNCEMENT',
          isSeen: false,
          actionUrl: '/tasks'
        },
        {
          user: req.user._id,
          message: 'System maintenance scheduled for Saturday 10:00 PM UTC.',
          type: 'SYSTEM',
          isSeen: false,
          actionUrl: '#'
        },
        {
          user: req.user._id,
          message: 'Weekly progress report template has been updated for this sprint.',
          type: 'REPORT_SUBMITTED',
          isSeen: true,
          seenAt: new Date(),
          actionUrl: '/tasks'
        }
      ];
      notifications = await Notification.insertMany(defaultNotifs);
    }

    res.json(notifications);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const getUnseenCount = async (req, res) => {
  try {
    const count = await Notification.countDocuments({
      user: req.user._id,
      isSeen: false
    });
    res.json({ count });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const markOneSeen = async (req, res) => {
  try {
    const notification = await Notification.findOne({
      _id: req.params.id,
      user: req.user._id
    });

    if (!notification) {
      return res.status(404).json({ message: 'Notification not found' });
    }

    notification.isSeen = true;
    notification.seenAt = new Date();
    await notification.save();

    res.json({ status: 'seen', notification });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const markAllSeen = async (req, res) => {
  try {
    await Notification.updateMany(
      { user: req.user._id, isSeen: false },
      { $set: { isSeen: true, seenAt: new Date() } }
    );
    res.json({ status: 'all_seen' });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const createNotification = async (req, res) => {
  try {
    const { message, type, actionUrl, recipientId } = req.body;
    const targetUserId = recipientId || req.user._id;

    const notif = await Notification.create({
      user: targetUserId,
      message,
      type: type || 'SYSTEM',
      actionUrl: actionUrl || '',
      isSeen: false
    });

    res.status(201).json(notif);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

module.exports = {
  getNotifications,
  getUnseenCount,
  markOneSeen,
  markAllSeen,
  createNotification
};
