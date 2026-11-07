const ChatMessage = require('../models/ChatMessage');
const User = require('../models/User');
const Notification = require('../models/Notification');

module.exports = (io) => {
  io.on('connection', (socket) => {
    let currentUserEmail = null;

    // Each user joins their private room identified by their unique email
    socket.on('addUser', async (payload) => {
      if (payload && payload.userEmail) {
        currentUserEmail = payload.userEmail;
        socket.join(currentUserEmail);
        console.log(`[Socket] User registered in private room: ${currentUserEmail} (socket: ${socket.id})`);

        // Notify others of online status
        io.emit('userStatusUpdate', {
          userEmail: currentUserEmail,
          status: 'online'
        });
      }
    });

    // 1-on-1 Strictly Private Messaging
    socket.on('sendMessage', async (payload) => {
      try {
        const { senderEmail, recipientEmail, content, clientMsgId } = payload;
        if (!senderEmail || !recipientEmail || !content || !content.trim()) {
          return;
        }

        // Look up users in DB to ensure validity
        const sender = await User.findOne({ email: senderEmail });
        const recipient = await User.findOne({ email: recipientEmail });

        if (!sender || !recipient) {
          socket.emit('chatError', { message: 'Sender or recipient not found' });
          return;
        }

        // Persist private message to MongoDB
        const savedMessage = await ChatMessage.create({
          sender: sender._id,
          recipient: recipient._id,
          senderEmail: sender.email,
          recipientEmail: recipient.email,
          content: content.trim(),
          isRead: false
        });

        const responsePayload = {
          _id: savedMessage._id,
          clientMsgId: clientMsgId || null,
          senderEmail: savedMessage.senderEmail,
          recipientEmail: savedMessage.recipientEmail,
          content: savedMessage.content,
          createdAt: savedMessage.createdAt,
          isRead: savedMessage.isRead
        };

        // Strictly private: Deliver only to the recipient's room and the sender's room
        io.to(recipientEmail).emit('messages', responsePayload);
        if (senderEmail !== recipientEmail) {
          io.to(senderEmail).emit('messages', responsePayload);
        }

        console.log(`[Private Message] From: ${senderEmail} -> To: ${recipientEmail}`);

        // Create a Notification document in DB for the recipient
        const snippet = content.trim().length > 50 ? content.trim().substring(0, 47) + '...' : content.trim();
        const notification = await Notification.create({
          user: recipient._id,
          message: `New message from ${sender.fullName || senderEmail}: "${snippet}"`,
          type: 'CHAT_MESSAGE',
          actionUrl: '/chat',
          isSeen: false
        });

        // Notify recipient in real-time to update their header notification bell
        io.to(recipientEmail).emit('newNotification', {
          notification,
          senderEmail,
          senderName: sender.fullName || senderEmail,
          message: notification.message
        });

      } catch (error) {
        console.error('[Socket Chat Error]:', error.message);
        socket.emit('chatError', { message: 'Failed to deliver private message' });
      }
    });

    socket.on('disconnect', () => {
      if (currentUserEmail) {
        console.log(`[Socket] User disconnected: ${currentUserEmail}`);
        io.emit('userStatusUpdate', {
          userEmail: currentUserEmail,
          status: 'offline'
        });
      }
    });
  });
};
