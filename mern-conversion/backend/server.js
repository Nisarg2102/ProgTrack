const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const dotenv = require('dotenv');
const http = require('http');

dotenv.config();

const app = express();

// CORS — allow your frontend origins
const allowedOrigins = [
  'http://localhost:5173',
  'http://localhost:3000',
  process.env.FRONTEND_URL
].filter(Boolean);

app.use(cors({
  origin: allowedOrigins,
  credentials: true
}));
app.use(express.json());

// ── Routes ──────────────────────────────────────────────
const authRoutes = require('./routes/authRoutes');
const taskRoutes = require('./routes/taskRoutes');
const workLogRoutes = require('./routes/workLogRoutes');
const notificationRoutes = require('./routes/notificationRoutes');
const analyticsRoutes = require('./routes/analyticsRoutes');
const chatRoutes = require('./routes/chatRoutes');

app.use('/api/auth', authRoutes);
app.use('/api/tasks', taskRoutes);
app.use('/api/worklogs', workLogRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/analytics', analyticsRoutes);
app.use('/api/chat', chatRoutes);

app.get('/', (req, res) => {
  res.send('ProgTrack MERN Backend API is running!');
});

// ── Database ────────────────────────────────────────────
const MONGO_URI = process.env.MONGO_URI || 'mongodb://localhost:27017/progtrack_mern';

mongoose.connect(MONGO_URI)
  .then(() => console.log('MongoDB connected'))
  .catch(err => console.error('MongoDB connection error:', err));

// ── Socket.IO (only works on traditional hosts, NOT Vercel serverless) ──
const PORT = process.env.PORT || 5000;
const server = http.createServer(app);

// Only attach Socket.IO when not running as a serverless function
if (!process.env.VERCEL) {
  const { Server } = require('socket.io');
  const io = new Server(server, {
    cors: {
      origin: allowedOrigins,
      methods: ['GET', 'POST'],
      credentials: true
    }
  });
  const chatSocket = require('./socket/chatSocket');
  app.set('io', io);
  chatSocket(io);
}

server.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});

// Export for Vercel serverless
module.exports = app;
