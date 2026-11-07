const ChatMessage = require('../models/ChatMessage');
const User = require('../models/User');
const Notification = require('../models/Notification');

// Fetch 1-on-1 private conversation between current user and specified recipient
const getPrivateConversation = async (req, res) => {
  try {
    const myEmail = req.user.email;
    const otherEmail = req.params.recipientEmail;

    if (!otherEmail) {
      return res.status(400).json({ message: 'Recipient email is required' });
    }

    const messages = await ChatMessage.find({
      $or: [
        { senderEmail: myEmail, recipientEmail: otherEmail },
        { senderEmail: otherEmail, recipientEmail: myEmail }
      ]
    })
      .sort({ createdAt: 1 })
      .limit(100);

    // Mark unread messages sent by other user as read
    await ChatMessage.updateMany(
      { senderEmail: otherEmail, recipientEmail: myEmail, isRead: false },
      { $set: { isRead: true, readAt: new Date() } }
    );

    res.json(messages);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// Send a private message (REST API fallback)
const sendPrivateMessage = async (req, res) => {
  try {
    const { recipientEmail, content, clientMsgId } = req.body;
    const sender = req.user;

    if (!recipientEmail || !content || !content.trim()) {
      return res.status(400).json({ message: 'Recipient email and message content are required' });
    }

    const recipient = await User.findOne({ email: recipientEmail });
    if (!recipient) {
      return res.status(404).json({ message: 'Recipient user not found' });
    }

    const message = await ChatMessage.create({
      sender: sender._id,
      recipient: recipient._id,
      senderEmail: sender.email,
      recipientEmail: recipient.email,
      content: content.trim(),
      isRead: false
    });

    // Create Notification document for recipient
    const snippet = content.trim().length > 50 ? content.trim().substring(0, 47) + '...' : content.trim();
    const notification = await Notification.create({
      user: recipient._id,
      message: `New message from ${sender.fullName || sender.email}: "${snippet}"`,
      type: 'CHAT_MESSAGE',
      actionUrl: '/chat',
      isSeen: false
    });

    // Emit real-time socket events if io is available
    const io = req.app.get('io');
    if (io) {
      const responsePayload = {
        _id: message._id,
        clientMsgId: clientMsgId || null,
        senderEmail: message.senderEmail,
        recipientEmail: message.recipientEmail,
        content: message.content,
        createdAt: message.createdAt,
        isRead: message.isRead
      };
      io.to(recipientEmail).emit('messages', responsePayload);
      io.to(recipientEmail).emit('newNotification', {
        notification,
        senderEmail: sender.email,
        senderName: sender.fullName || sender.email,
        message: notification.message
      });
    }

    res.status(201).json(message);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

module.exports = {
  getPrivateConversation,
  sendPrivateMessage
};
