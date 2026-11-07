const express = require('express');
const router = express.Router();
const {
  getPrivateConversation,
  sendPrivateMessage
} = require('../controllers/chatController');
const { protect } = require('../middleware/authMiddleware');

router.get('/conversation/:recipientEmail', protect, getPrivateConversation);
router.post('/send', protect, sendPrivateMessage);

module.exports = router;
