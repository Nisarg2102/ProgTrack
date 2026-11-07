const mongoose = require('mongoose');

const workLogSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  loginDateTime: {
    type: Date,
    required: true
  },
  logoutDateTime: {
    type: Date
  },
  totalSeconds: {
    type: Number
  },
  sessionId: {
    type: String,
    maxlength: 100
  },
  ipAddress: {
    type: String,
    maxlength: 50
  }
}, {
  timestamps: true
});

workLogSchema.methods.calculateTotalSeconds = function() {
  if (this.loginDateTime && this.logoutDateTime) {
    const diff = this.logoutDateTime.getTime() - this.loginDateTime.getTime();
    this.totalSeconds = Math.floor(diff / 1000);
  }
};

const WorkLog = mongoose.model('WorkLog', workLogSchema);
module.exports = WorkLog;
