const mongoose = require('mongoose');

const notificationSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  message: {
    type: String,
    required: [true, 'Notification message is required']
  },
  type: {
    type: String,
    enum: [
      'LEAVE_APPROVED', 'LEAVE_REJECTED', 'LEAVE_REQUESTED',
      'TASK_REMINDER', 'TASK_SUBMITTED', 'TASK_REVIEWED',
      'CHAT_MESSAGE', 'SYSTEM', 'ANNOUNCEMENT',
      'REPORT_SUBMITTED', 'REPORT_REVIEWED',
      'REPORT_DEADLINE_REMINDER', 'SYSTEM_ALERT'
    ],
    default: 'SYSTEM'
  },
  isSeen: {
    type: Boolean,
    default: false
  },
  actionUrl: {
    type: String,
    default: ''
  },
  seenAt: {
    type: Date
  }
}, {
  timestamps: true
});

const Notification = mongoose.model('Notification', notificationSchema);
module.exports = Notification;
