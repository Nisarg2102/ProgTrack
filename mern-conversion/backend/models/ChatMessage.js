const mongoose = require('mongoose');

const chatMessageSchema = new mongoose.Schema({
  sender: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  recipient: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  senderEmail: {
    type: String,
    required: true
  },
  recipientEmail: {
    type: String,
    required: true
  },
  content: {
    type: String,
    required: [true, 'Message content cannot be empty']
  },
  isRead: {
    type: Boolean,
    default: false
  },
  readAt: {
    type: Date
  }
}, {
  timestamps: true
});

// Index for high-performance lookup of private conversations between two users
chatMessageSchema.index({ sender: 1, recipient: 1 });
chatMessageSchema.index({ senderEmail: 1, recipientEmail: 1 });

const ChatMessage = mongoose.model('ChatMessage', chatMessageSchema);
module.exports = ChatMessage;
