const mongoose = require('mongoose');

const dailyTaskSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  taskDate: {
    type: Date,
    required: [true, 'Task date is required']
  },
  taskDescription: {
    type: String,
    required: [true, 'Task description is required']
  },
  timeSpent: {
    type: Number
  },
  status: {
    type: String,
    required: true,
    enum: ['IN_PROGRESS', 'COMPLETED', 'PENDING', 'BLOCKED', 'REVIEW'],
    default: 'IN_PROGRESS',
    maxlength: 20
  },
  googleDocLink: {
    type: String,
    maxlength: 512
  },
  taskCategory: {
    type: String,
    maxlength: 100
  },
  priority: {
    type: String,
    default: 'MEDIUM',
    maxlength: 20
  },
  submitted: {
    type: Boolean,
    default: false
  }
}, {
  timestamps: true
});

const DailyTask = mongoose.model('DailyTask', dailyTaskSchema);
module.exports = DailyTask;
